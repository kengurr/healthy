package com.zdravdom.cms.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service offered in the platform (e.g., wound care, physiotherapy session).
 */
public record Service(
    Long id,
    String name,
    ServiceCategory category,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    Double rating,
    String imageUrl,
    List<String> includedItems,
    List<String> requiredDocuments,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public enum ServiceCategory {
        NURSING_CARE,
        PHYSIOTHERAPY,
        MEDICAL_CONSULTATION,
        WOUND_CARE,
        POST_SURGERY_CARE,
        ELDERLY_CARE,
        PEDIATRIC_CARE,
        CHRONIC_DISEASE_MONITORING,
        PALLIATIVE_CARE,
        LABORATORY_SERVICES
    }

    public Service {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be null or blank");
        }
        if (includedItems == null) includedItems = List.of();
        if (requiredDocuments == null) requiredDocuments = List.of();
        if (active) active = true;
    }

    public boolean isHourly() {
        return durationMinutes != null && durationMinutes >= 60;
    }

    public BigDecimal getHourlyRate() {
        if (price == null || durationMinutes == null || durationMinutes == 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(60L)).divide(BigDecimal.valueOf(durationMinutes), 2, java.math.RoundingMode.HALF_UP);
    }
}
