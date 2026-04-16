package com.zdravdom.cms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service offered in the platform (e.g., wound care, physiotherapy session).
 * Maps to cms.services table.
 */
@Entity
@Table(name = "services", schema = "cms")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // Default constructor for JPA
    public Service() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public ServiceCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public Double getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String[] getIncludedItems() { return includedItems; }
    public String[] getRequiredDocuments() { return requiredDocuments; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(ServiceCategory category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setRating(Double rating) { this.rating = rating; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setIncludedItems(String[] includedItems) { this.includedItems = includedItems; }
    public void setRequiredDocuments(String[] requiredDocuments) { this.requiredDocuments = requiredDocuments; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
