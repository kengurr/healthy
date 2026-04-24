package com.zdravdom.matching.adapters.out.persistence;

import com.zdravdom.matching.domain.ProviderLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for ProviderLocation with Haversine radius queries.
 * Uses plain DOUBLE PRECISION lat/lng columns — no PostGIS required.
 */
@Repository
public interface ProviderLocationRepository extends JpaRepository<ProviderLocation, Long> {

    List<ProviderLocation> findByProviderId(Long providerId);

    /**
     * Find provider locations within radius (km) of a lat/lng point,
     * sorted by distance ascending using Haversine formula.
     */
    @Query(value = """
        SELECT pl.* FROM matching.provider_locations pl
        WHERE (
            6371.0 * acos(
                LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(pl.latitude)) *
                    cos(radians(pl.longitude) - radians(:lng))
                )
            )
        ) <= :radius_km
        ORDER BY (
            6371.0 * acos(
                LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(pl.latitude)) *
                    cos(radians(pl.longitude) - radians(:lng))
                )
            )
        )
        """, nativeQuery = true)
    List<ProviderLocation> findWithinRadius(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radius_km") double radiusKm
    );

    /**
     * Find provider locations within radius (km) of a point, filtered by service category
     * (joined via providers table). Sorted by distance.
     */
    @Query(value = """
        SELECT pl.* FROM matching.provider_locations pl
        JOIN "user".providers p ON p.id = pl.provider_id
        WHERE (
            6371.0 * acos(
                LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(pl.latitude)) *
                    cos(radians(pl.longitude) - radians(:lng))
                )
            )
        ) <= :radius_km
        AND p.category = :category
        AND p.status = 'ACTIVE'
        ORDER BY (
            6371.0 * acos(
                LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(pl.latitude)) *
                    cos(radians(pl.longitude) - radians(:lng))
                )
            )
        )
        """, nativeQuery = true)
    List<ProviderLocation> findWithinRadiusByCategory(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radius_km") double radiusKm,
        @Param("category") String category
    );
}