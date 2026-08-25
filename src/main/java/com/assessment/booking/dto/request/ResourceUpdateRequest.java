package com.assessment.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceUpdateRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 150, message = "Resource name must not exceed 150 characters")
    private String name;

    private String description;

    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price per hour must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Price must be a valid decimal amount with up to 2 decimal places")
    private BigDecimal pricePerHour;

    private Boolean active;
}
