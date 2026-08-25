package com.assessment.booking.service;

import com.assessment.booking.dto.request.ReservationCreateRequest;
import com.assessment.booking.dto.response.ReservationResponse;
import com.assessment.booking.entity.Reservation;
import com.assessment.booking.entity.ReservationStatus;
import com.assessment.booking.entity.Resource;
import com.assessment.booking.entity.Role;
import com.assessment.booking.entity.User;
import com.assessment.booking.exception.BadRequestException;
import com.assessment.booking.exception.ConflictException;
import com.assessment.booking.exception.ForbiddenException;
import com.assessment.booking.exception.ResourceNotFoundException;
import com.assessment.booking.repository.ReservationRepository;
import com.assessment.booking.repository.ResourceRepository;
import com.assessment.booking.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User regularUser;
    private User otherUser;
    private User adminUser;
    private Resource resource;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .fullName("Alice")
                .role(Role.ROLE_USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("bob@example.com")
                .fullName("Bob")
                .role(Role.ROLE_USER)
                .build();

        adminUser = User.builder()
                .id(3L)
                .email("admin@example.com")
                .fullName("Admin")
                .role(Role.ROLE_ADMIN)
                .build();

        resource = Resource.builder()
                .id(10L)
                .name("Conference Room A")
                .pricePerHour(new BigDecimal("100.00"))
                .active(true)
                .capacity(10)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a reservation and calculate total price correctly")
    void testCreateReservation_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2); // 2 hours = $200.00

        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .notes("Team Sync")
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(regularUser);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(reservationRepository.findOverlappingReservations(eq(10L), eq(start), eq(end), anyList()))
                .thenReturn(Collections.emptyList());

        Reservation savedReservation = Reservation.builder()
                .id(100L)
                .user(regularUser)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .status(ReservationStatus.PENDING)
                .totalPrice(new BigDecimal("200.00"))
                .notes("Team Sync")
                .build();

        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");

        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when requested reservation overlaps with existing booking")
    void testCreateReservation_OverlapConflict() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(regularUser);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        Reservation existingReservation = Reservation.builder()
                .id(99L)
                .user(otherUser)
                .resource(resource)
                .startTime(start.plusMinutes(30))
                .endTime(end.plusMinutes(30))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findOverlappingReservations(eq(10L), eq(start), eq(end), anyList()))
                .thenReturn(List.of(existingReservation));

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Resource is already booked");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user tries to access another user's reservation")
    void testGetReservationById_ForbiddenForOtherUser() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(otherUser);

        Reservation reservation = Reservation.builder()
                .id(100L)
                .user(regularUser) // Owned by Alice
                .resource(resource)
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(new BigDecimal("100.00"))
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(100L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have permission");
    }

    @Test
    @DisplayName("Admin should be able to view any user's reservation")
    void testGetReservationById_AdminAllowed() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(adminUser);

        Reservation reservation = Reservation.builder()
                .id(100L)
                .user(regularUser) // Owned by Alice
                .resource(resource)
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(new BigDecimal("100.00"))
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getReservationById(100L);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should throw BadRequestException if end time is before start time")
    void testCreateReservation_InvalidTimes() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        LocalDateTime end = start.minusHours(1); // Invalid: before start

        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(regularUser);

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time must be strictly after start time");
    }
}
