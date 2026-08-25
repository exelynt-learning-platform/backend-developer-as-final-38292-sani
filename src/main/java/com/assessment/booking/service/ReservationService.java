package com.assessment.booking.service;

import com.assessment.booking.dto.request.ReservationCreateRequest;
import com.assessment.booking.dto.request.ReservationStatusUpdateRequest;
import com.assessment.booking.dto.request.ReservationUpdateRequest;
import com.assessment.booking.dto.response.PagedResponse;
import com.assessment.booking.dto.response.ReservationResponse;
import com.assessment.booking.entity.ReservationStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ReservationService {

    ReservationResponse createReservation(ReservationCreateRequest request);

    ReservationResponse getReservationById(Long id);

    PagedResponse<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long resourceId,
            LocalDateTime startAfter,
            LocalDateTime endBefore,
            Long filterUserId,
            Pageable pageable
    );

    ReservationResponse updateReservation(Long id, ReservationUpdateRequest request);

    ReservationResponse updateReservationStatus(Long id, ReservationStatusUpdateRequest request);

    ReservationResponse cancelReservation(Long id);

    void deleteReservation(Long id);
}
