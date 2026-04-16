package com.zdravdom.cms.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service package (S/M/L bundles) for recurring care.
 */
public record ServicePackage(
    Long id,
    String name,
    PackageSize size,
    String description,
    List<Long> serviceIds,
    BigDecimal price,
    BigDecimal discountPercent,
    Integer validityDays,
    List<String> benefits,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public enum PackageSize {
        S,  // Small - 1-3 visits
        M,  // Medium - 4-8 visits
        L   // Large - 9+ visits
    }

    public ServicePackage {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Package name cannot be null or blank");
        }
        if (serviceIds == null) serviceIds = List.of();
        if (benefits == null) benefits = List.of();
        if (discountPercent == null) discountPercent = BigDecimal.ZERO;
        if (active) active = true;
    }

    public BigDecimal getDiscountedPrice() {
        if (price == null || discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        return price.multiply(BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
    }

    public int getVisitCount() {
        return size == PackageSize.S ? 3 : size == PackageSize.M ? 8 : 12; // Default estimates
    }
}
