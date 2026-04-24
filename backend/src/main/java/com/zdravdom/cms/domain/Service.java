package com.zdravdom.cms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service offered in the platform (e.g., wound care, physiotherapy session).
 * Maps to cms.services table.
 *
 * PRODUCTION: Missing index on category — used in CMS service filtering.
 * PRODUCTION: Missing index on active — used in "active services only" queries.
 * PRODUCTION: Missing composite index on (active, category) — common query pattern.
 */
@Entity
@Table(name = "services", schema = "cms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private BigDecimal price;

    private Double rating = 0.0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "included_items", columnDefinition = "TEXT[]")
    private String[] includedItems;

    @Column(name = "required_documents", columnDefinition = "TEXT[]")
    private String[] requiredDocuments;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ServiceCategory {
        NURSING_CARE, PHYSIOTHERAPY, MEDICAL_CONSULTATION, WOUND_CARE,
        POST_SURGERY_CARE, ELDERLY_CARE, PEDIATRIC_CARE,
        CHRONIC_DISEASE_MONITORING, PALLIATIVE_CARE, LABORATORY_SERVICES
    }

    /** Factory method for application-layer and test instantiation. */
    public static Service create() {
        return new Service();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (uuid == null) uuid = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isHourly() {
        return durationMinutes != null && durationMinutes >= 60;
    }

    public BigDecimal getHourlyRate() {
        if (price == null || durationMinutes == null || durationMinutes == 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(60L))
                .divide(BigDecimal.valueOf(durationMinutes), 2, java.math.RoundingMode.HALF_UP);
    }
}
