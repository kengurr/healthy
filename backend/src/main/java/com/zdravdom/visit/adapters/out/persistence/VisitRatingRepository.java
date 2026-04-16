package com.zdravdom.visit.adapters.out.persistence;

import com.zdravdom.visit.domain.VisitRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for VisitRating entities.
 */
@Repository
public interface VisitRatingRepository extends JpaRepository<VisitRating, Long> {

    Optional<VisitRating> findByVisitId(Long visitId);

    boolean existsByVisitId(Long visitId);

    @Query("SELECT AVG(r.rating) FROM VisitRating r WHERE r.providerId = :providerId")
    Double findAverageRatingByProviderId(@Param("providerId") Long providerId);

    int countByProviderId(Long providerId);
}
