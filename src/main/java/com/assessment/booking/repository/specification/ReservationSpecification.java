package com.assessment.booking.repository.specification;

import com.assessment.booking.entity.Reservation;
import com.assessment.booking.entity.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    public static Specification<Reservation> withDynamicFilters(
            Long userId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long resourceId,
            LocalDateTime startAfter,
            LocalDateTime endBefore
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalPrice"), maxPrice));
            }

            if (resourceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("resource").get("id"), resourceId));
            }

            if (startAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), startAfter));
            }

            if (endBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), endBefore));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
