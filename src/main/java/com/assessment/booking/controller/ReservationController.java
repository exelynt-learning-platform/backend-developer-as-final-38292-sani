package com.assessment.booking.controller;

import com.assessment.booking.dto.request.ReservationCreateRequest;
import com.assessment.booking.dto.request.ReservationStatusUpdateRequest;
import com.assessment.booking.dto.request.ReservationUpdateRequest;
import com.assessment.booking.dto.response.ApiResponse;
import com.assessment.booking.dto.response.PagedResponse;
import com.assessment.booking.dto.response.ReservationResponse;
import com.assessment.booking.entity.ReservationStatus;
import com.assessment.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "3. Reservations", description = "Endpoints for creating, managing, filtering, and cancelling reservations")
@SecurityRequirement(name = "BearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a new reservation",
               description = "Creates a reservation for the authenticated user. Note: The user identity is securely extracted from the JWT token, not from the request body.")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        ReservationResponse response = reservationService.createReservation(request);
        return new ResponseEntity<>(ApiResponse.success("Reservation created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get reservations with filtering, pagination, and sorting",
               description = "Returns paginated reservations. USER role sees only their own reservations. ADMIN role sees all reservations or can filter by specific user.")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationResponse>>> getReservations(
            @Parameter(description = "Filter by status: PENDING, CONFIRMED, CANCELLED")
            @RequestParam(required = false) ReservationStatus status,

            @Parameter(description = "Filter by minimum total price")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Filter by maximum total price")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Filter by resource ID")
            @RequestParam(required = false) Long resourceId,

            @Parameter(description = "Filter reservations starting after (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAfter,

            @Parameter(description = "Filter reservations ending before (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endBefore,

            @Parameter(description = "Filter by user ID (ADMIN only)")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by (e.g. createdAt, totalPrice, startTime)")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<ReservationResponse> response = reservationService.getReservations(
                status,
                minPrice,
                maxPrice,
                resourceId,
                startAfter,
                endBefore,
                userId,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID",
               description = "Returns reservation details. USER can only access their own reservation. ADMIN can access any reservation.")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update reservation details",
               description = "Updates reservation times and notes. USER can only update their own PENDING reservation. ADMIN can update any reservation.")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request
    ) {
        ReservationResponse response = reservationService.updateReservation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reservation updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update reservation status (ADMIN Only)",
               description = "Allows ADMIN to transition reservation status between PENDING, CONFIRMED, and CANCELLED.")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusUpdateRequest request
    ) {
        ReservationResponse response = reservationService.updateReservationStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reservation status updated to " + request.getStatus(), response));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation",
               description = "Cancels a reservation. USER can cancel their own reservation. ADMIN can cancel any reservation.")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.cancelReservation(id);
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN Only)",
               description = "Allows ADMIN to permanently delete a reservation record.")
    public ResponseEntity<ApiResponse<Void>> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.ok(ApiResponse.success("Reservation deleted successfully", null));
    }
}
