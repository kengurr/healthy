package com.zdravdom.visit.adapters.out.persistence;

import com.zdravdom.visit.domain.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Visit entities.
 */
@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByPatientId(Long patientId);

    List<Visit> findByProviderId(Long providerId);

    List<Visit> findByBookingId(Long bookingId);

    List<Visit> findByStatus(Visit.VisitStatus status);
}