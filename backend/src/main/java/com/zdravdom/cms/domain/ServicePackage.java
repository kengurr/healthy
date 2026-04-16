package com.zdravdom.cms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service package (S/M/L bundles) for recurring care.
 * Maps to cms.service_packages table.
 */
@Entity
@Table(name = "service_packages", schema = "cms")
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

    // Default constructor for JPA
    public ServicePackage() {}

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

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public PackageSize getSize() { return size; }
    public String getDescription() { return description; }
    public Long[] getServiceIds() { return serviceIds; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public Integer getValidityDays() { return validityDays; }
    public String[] getBenefits() { return benefits; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSize(PackageSize size) { this.size = size; }
    public void setDescription(String description) { this.description = description; }
    public void setServiceIds(Long[] serviceIds) { this.serviceIds = serviceIds; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }
    public void setBenefits(String[] benefits) { this.benefits = benefits; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
