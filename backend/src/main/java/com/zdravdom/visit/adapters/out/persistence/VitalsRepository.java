package com.zdravdom.visit.adapters.out.persistence;

import com.zdravdom.visit.domain.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for Vitals entities.
 */
@Repository
public interface VitalsRepository extends JpaRepository<Vitals, Long> {

    Optional<Vitals> findByVisitId(Long visitId);
}
