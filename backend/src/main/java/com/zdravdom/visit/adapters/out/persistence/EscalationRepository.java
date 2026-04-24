package com.zdravdom.visit.adapters.out.persistence;

import com.zdravdom.visit.domain.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Escalation entities.
 */
@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    List<Escalation> findByVisitId(Long visitId);

    List<Escalation> findByStatus(Escalation.EscalationStatus status);
}
