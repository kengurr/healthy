package com.zdravdom.cms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service package (S/M/L bundles) for recurring care.
 * Maps to cms.service_packages table.
 */
@Entity
@Table(name = "service_packages", schema = "cms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageSize size;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_ids", columnDefinition = "BIGINT[]")
    private Long[] serviceIds;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(columnDefinition = "TEXT[]")
    private String[] benefits;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PackageSize {
        S, M, L
    }

    /** Factory method for application-layer and test instantiation. */
    public static ServicePackage create() {
        return new ServicePackage();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getDiscountedPrice() {
        if (price == null || discountPercent == null ||
            discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        return price.multiply(
            BigDecimal.ONE.subtract(
                discountPercent.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
            )
        );
    }

    public int getVisitCount() {
        return size == PackageSize.S ? 3 : size == PackageSize.M ? 8 : 12;
    }
}
