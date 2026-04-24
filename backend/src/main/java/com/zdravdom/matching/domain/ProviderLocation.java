package com.zdravdom.matching.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Provider service location for geo-matching.
 * Maps to matching.provider_locations table.
 *
 * Coordinates stored as plain DOUBLE PRECISION (latitude, longitude) in WGS84 (SRID 4326).
 * Geo-queries use Haversine formula computed in SQL — no PostGIS extension required.
 * For production scale, consider PostGIS with GiST index or PostgreSQL earthdistance extension.
 */
@Entity
@Table(name = "provider_locations", schema = "matching")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "latitude", nullable = false)
    private Double latitude = 0.0;

    @Column(name = "longitude", nullable = false)
    private Double longitude = 0.0;

    @Column(name = "service_radius_km", precision = 5, scale = 2)
    private BigDecimal serviceRadiusKm = BigDecimal.valueOf(25.0);

    @Column(name = "is_primary")
    private boolean isPrimary = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /** Factory method for application-layer instantiation. */
    public static ProviderLocation create() {
        return new ProviderLocation();
    }
}