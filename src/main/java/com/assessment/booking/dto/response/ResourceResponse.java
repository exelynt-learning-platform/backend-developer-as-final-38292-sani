package com.assessment.booking.dto.response;

import com.assessment.booking.entity.Resource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private Integer capacity;
    private String location;
    private BigDecimal pricePerHour;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResourceResponse fromEntity(Resource resource) {
        if (resource == null) return null;
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .capacity(resource.getCapacity())
                .location(resource.getLocation())
                .pricePerHour(resource.getPricePerHour())
                .active(resource.getActive())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
