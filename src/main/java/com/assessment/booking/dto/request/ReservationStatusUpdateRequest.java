package com.assessment.booking.dto.request;

import com.assessment.booking.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatusUpdateRequest {

    @NotNull(message = "Status is required (PENDING, CONFIRMED, CANCELLED)")
    private ReservationStatus status;
}
