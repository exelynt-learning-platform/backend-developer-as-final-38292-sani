package com.assessment.booking.dto.response;

import com.assessment.booking.entity.Reservation;
import com.assessment.booking.entity.ReservationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long id;
    private UserResponse user;
    private ResourceResponse resource;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReservationResponse fromEntity(Reservation reservation) {
        if (reservation == null) return null;
        return ReservationResponse.builder()
                .id(reservation.getId())
                .user(UserResponse.fromEntity(reservation.getUser()))
                .resource(ResourceResponse.fromEntity(reservation.getResource()))
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .totalPrice(reservation.getTotalPrice())
                .notes(reservation.getNotes())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
