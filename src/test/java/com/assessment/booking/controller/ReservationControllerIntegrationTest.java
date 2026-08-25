package com.assessment.booking.controller;

import com.assessment.booking.dto.request.ReservationCreateRequest;
import com.assessment.booking.dto.request.ReservationStatusUpdateRequest;
import com.assessment.booking.entity.ReservationStatus;
import com.assessment.booking.entity.Resource;
import com.assessment.booking.repository.ReservationRepository;
import com.assessment.booking.repository.ResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("POST /reservations - Authenticated USER can create reservation, identity taken from JWT")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testCreateReservation_UserSuccess() throws Exception {
        Resource resource = resourceRepository.findByActiveTrue().get(0);
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .resourceId(resource.getId())
                .startTime(start)
                .endTime(end)
                .notes("Integration test reservation")
                .build();

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.email", is("user@example.com")))
                .andExpect(jsonPath("$.data.resource.id", is(resource.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.totalPrice", notNullValue()));
    }

    @Test
    @DisplayName("GET /reservations - Regular USER can view ONLY their own reservations")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testGetReservations_UserOwnershipSegregation() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].user.email", everyItem(is("user@example.com"))));
    }

    @Test
    @DisplayName("GET /reservations - ADMIN can view reservations across multiple users")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testGetReservations_AdminCanViewAll() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }

    @Test
    @DisplayName("GET /reservations - Filtering by status, minPrice, and maxPrice")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testGetReservations_Filtering() throws Exception {
        mockMvc.perform(get("/reservations")
                        .param("status", "CONFIRMED")
                        .param("minPrice", "100.00")
                        .param("maxPrice", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].status", everyItem(is("CONFIRMED"))));
    }

    @Test
    @DisplayName("GET /reservations - Pagination and Sorting parameters")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testGetReservations_PaginationAndSorting() throws Exception {
        mockMvc.perform(get("/reservations")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "totalPrice")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pageSize", is(2)))
                .andExpect(jsonPath("$.data.pageNumber", is(0)));
    }

    @Test
    @DisplayName("POST /reservations - Should reject overlapping reservation with 409 Conflict")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testCreateReservation_OverlapConflictDetection() throws Exception {
        Resource resource = resourceRepository.findByActiveTrue().get(0);
        LocalDateTime start = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        ReservationCreateRequest firstRequest = ReservationCreateRequest.builder()
                .resourceId(resource.getId())
                .startTime(start)
                .endTime(end)
                .notes("First reservation")
                .build();

        // 1. Create first reservation
        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // 2. Try creating overlapping reservation on the same resource
        ReservationCreateRequest conflictingRequest = ReservationCreateRequest.builder()
                .resourceId(resource.getId())
                .startTime(start.plusMinutes(30))
                .endTime(end.plusMinutes(30))
                .notes("Conflicting reservation")
                .build();

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already booked")));
    }

    @Test
    @DisplayName("PATCH /reservations/{id}/status - ADMIN can update status to CONFIRMED")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testUpdateReservationStatus_AdminSuccess() throws Exception {
        var reservations = reservationRepository.findAll();
        var pendingReservation = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .findFirst()
                .orElse(reservations.get(0));

        ReservationStatusUpdateRequest request = ReservationStatusUpdateRequest.builder()
                .status(ReservationStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/reservations/" + pendingReservation.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("PATCH /reservations/{id}/status - Regular USER gets 403 Forbidden")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testUpdateReservationStatus_UserForbidden() throws Exception {
        var reservation = reservationRepository.findAll().get(0);

        ReservationStatusUpdateRequest request = ReservationStatusUpdateRequest.builder()
                .status(ReservationStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/reservations/" + reservation.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
