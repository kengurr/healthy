package com.zdravdom.user.adapters.out.persistence;

import com.zdravdom.user.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for Patient entities.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByUserId(Long userId);

    boolean existsByEmail(String email);
}