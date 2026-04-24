package com.zdravdom.user.application.service;

import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.application.dto.AdminPatientResponse;
import com.zdravdom.user.domain.Patient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin service for patient/user management.
 */
@Service
public class AdminUserService {

    private final PatientRepository patientRepository;

    public AdminUserService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    private AdminPatientResponse toResponse(Patient patient) {
        return new AdminPatientResponse(
            patient.getId(),
            patient.getEmail(),
            patient.getPhone(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getDateOfBirth(),
            patient.getGender() != null ? patient.getGender().name() : null,
            patient.isVerified(),
            patient.isActive(),
            patient.getCreatedAt()
        );
    }
}
