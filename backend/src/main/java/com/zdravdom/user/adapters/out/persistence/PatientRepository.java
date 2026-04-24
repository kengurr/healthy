package com.zdravdom.user.adapters.out.persistence;

import com.zdravdom.user.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * JPA repository for Patient entities.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByUserId(Long userId);

    boolean existsByEmail(String email);

    /**
     * Fetch the primary address coordinates for a patient.
     * Returns latitude or null if not found.
     */
    @Query(value = """
        SELECT a.latitude FROM "user".addresses a
        WHERE a.patient_id = :patientId AND a.is_primary = true
        LIMIT 1
        """, nativeQuery = true)
    BigDecimal findPrimaryAddressLatitude(@Param("patientId") Long patientId);

    @Query(value = """
        SELECT a.longitude FROM "user".addresses a
        WHERE a.patient_id = :patientId AND a.is_primary = true
        LIMIT 1
        """, nativeQuery = true)
    BigDecimal findPrimaryAddressLongitude(@Param("patientId") Long patientId);
}