package com.assessment.booking.service.impl;

import com.assessment.booking.dto.request.ReservationCreateRequest;
import com.assessment.booking.dto.request.ReservationStatusUpdateRequest;
import com.assessment.booking.dto.request.ReservationUpdateRequest;
import com.assessment.booking.dto.response.PagedResponse;
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
import com.assessment.booking.repository.specification.ReservationSpecification;
import com.assessment.booking.service.AuthService;
import com.assessment.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final AuthService authService;

    private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED
    );

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        log.info("User {} (ID: {}) is creating reservation for resource ID: {}",
                currentUser.getEmail(), currentUser.getId(), request.getResourceId());

        validateDates(request.getStartTime(), request.getEndTime());

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId()));

        if (Boolean.FALSE.equals(resource.getActive())) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently inactive and cannot be booked");
        }

        // Check for conflicting reservations on the same resource
        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                ACTIVE_STATUSES
        );

        if (!conflicts.isEmpty()) {
            throw new ConflictException("Resource is already booked or pending for the requested time interval");
        }

        BigDecimal totalPrice = calculateTotalPrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime());

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .totalPrice(totalPrice)
                .notes(request.getNotes())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Successfully created reservation ID: {} with price: {}", saved.getId(), saved.getTotalPrice());
        return ReservationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        log.info("Fetching reservation ID: {} for user: {}", id, currentUser.getEmail());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));

        // Enforce RBAC ownership check
        if (!isAdmin(currentUser) && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to view this reservation");
        }

        return ReservationResponse.fromEntity(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long resourceId,
            LocalDateTime startAfter,
            LocalDateTime endBefore,
            Long filterUserId,
            Pageable pageable
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        log.info("Querying reservations - user: {}, role: {}, status: {}, minPrice: {}, maxPrice: {}",
                currentUser.getEmail(), currentUser.getRole(), status, minPrice, maxPrice);

        Long effectiveUserId;
        if (isAdmin(currentUser)) {
            effectiveUserId = filterUserId; // Admin can filter by any user or see all (if null)
        } else {
            effectiveUserId = currentUser.getId(); // USER can ONLY see their own reservations
        }

        Specification<Reservation> spec = ReservationSpecification.withDynamicFilters(
                effectiveUserId,
                status,
                minPrice,
                maxPrice,
                resourceId,
                startAfter,
                endBefore
        );

        Page<Reservation> page = reservationRepository.findAll(spec, pageable);

        List<ReservationResponse> responses = page.getContent()
                .stream()
                .map(ReservationResponse::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(page, responses);
    }

    @Override
    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        log.info("Updating reservation ID: {} by user: {}", id, currentUser.getEmail());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));

        // RBAC check
        if (!isAdmin(currentUser)) {
            if (!reservation.getUser().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You do not have permission to modify this reservation");
            }
            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                throw new BadRequestException("Cancelled reservations cannot be modified");
            }
        }

        validateDates(request.getStartTime(), request.getEndTime());

        // Check for conflicting reservations excluding this one
        List<Reservation> conflicts = reservationRepository.findOverlappingReservationsExcluding(
                reservation.getResource().getId(),
                id,
                request.getStartTime(),
                request.getEndTime(),
                ACTIVE_STATUSES
        );

        if (!conflicts.isEmpty()) {
            throw new ConflictException("Resource is already booked or pending for the requested updated time interval");
        }

        BigDecimal updatedPrice = calculateTotalPrice(
                reservation.getResource().getPricePerHour(),
                request.getStartTime(),
                request.getEndTime()
        );

        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setTotalPrice(updatedPrice);
        if (request.getNotes() != null) {
            reservation.setNotes(request.getNotes());
        }

        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatusUpdateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        log.info("Admin {} is updating status of reservation ID: {} to {}",
                currentUser.getEmail(), id, request.getStatus());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));

        reservation.setStatus(request.getStatus());
        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        log.info("Cancelling reservation ID: {} by user: {}", id, currentUser.getEmail());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));

        // Ownership check
        if (!isAdmin(currentUser) && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteReservation(Long id) {
        log.info("Deleting reservation ID: {}", id);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
        reservationRepository.delete(reservation);
    }

    private void validateDates(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time must be specified");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be strictly after start time");
        }
        if (Duration.between(startTime, endTime).toMinutes() < 15) {
            throw new BadRequestException("Reservation duration must be at least 15 minutes");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reservation start time must be in the future");
        }
    }

    private BigDecimal calculateTotalPrice(BigDecimal pricePerHour, LocalDateTime startTime, LocalDateTime endTime) {
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(durationMinutes)
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ROLE_ADMIN;
    }
}
