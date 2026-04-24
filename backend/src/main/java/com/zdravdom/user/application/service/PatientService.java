package com.zdravdom.user.application.service;

import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.application.dto.AddressRequest;
import com.zdravdom.user.application.dto.PatientResponse;
import com.zdravdom.user.application.dto.PatientResponse.AddressResponse;
import com.zdravdom.user.application.dto.PatientResponse.EmergencyContactResponse;
import com.zdravdom.user.application.dto.PatientResponse.InsuranceDetailsResponse;
import com.zdravdom.user.application.dto.UpdatePatientRequest;
import com.zdravdom.user.application.mapper.PatientMapper;
import com.zdravdom.user.domain.Address;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Patient.EmergencyContact;
import com.zdravdom.user.domain.Patient.InsuranceDetails;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for patient profile management.
 * All operations are backed by the real database with optimistic locking
 * (@Version) and dynamic SQL updates (@DynamicUpdate) for correctness and performance.
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientByUserId(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", userId));
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(Long userId, UpdatePatientRequest request) {
        Patient patient = patientRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", userId));

        PatientMapper.updateFromRequest(patient, request);

        Patient saved = patientRepository.save(patient);
        log.info("Updated patient profile for userId: {}", userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", userId));
        Address addr = patient.getAddress();
        if (addr == null || addr.getStreet() == null || addr.getStreet().isBlank()) {
            return List.of();
        }
        return List.of(toAddressResponse(patient));
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        Patient patient = patientRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", userId));

        PatientMapper.updateAddressFromRequest(patient, request);

        Patient saved = patientRepository.save(patient);
        log.info("Added address for patient userId: {}", userId);
        return toAddressResponse(saved);
    }

    @Transactional(readOnly = true)
    public GDPRExportResponse exportGdprData(Long userId) {
        patientRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", userId));

        // TODO (Production): Replace with a real async job:
        //   1. Persist a GDPR job record (GdprExportJob entity: userId, status, createdAt, fileKey)
        //   2. Emit a Spring ApplicationEvent (or publish to Kafka/SQS)
        //   3. GdprExportWorker listens, builds ZIP of patient data, uploads to S3
        //   4. Updates job record status → COMPLETED with downloadUrl
        //   5. This endpoint polls job status by jobId (separate GET /gdpr/jobs/{jobId})
        //
        // For now, we return a placeholder job that immediately reports PROCESSING.
        // The caller should poll a future GET /gdpr/jobs/{jobId} endpoint.
        String jobId = java.util.UUID.randomUUID().toString();
        log.info("Started GDPR export for userId: {}, jobId: {} (placeholder — implement async job worker for production)", userId, jobId);
        return new GDPRExportResponse(java.util.UUID.fromString(jobId), "PROCESSING", null);
    }

    public record GDPRExportResponse(java.util.UUID jobId, String status, String downloadUrl) {}

    private PatientResponse toResponse(Patient patient) {
        Address addr = patient.getAddress();
        AddressResponse addrResp = (addr != null && addr.getStreet() != null)
            ? toAddressResponse(patient) : null;

        InsuranceDetailsResponse insResp = null;
        if (patient.getInsuranceDetails() != null) {
            InsuranceDetails d = patient.getInsuranceDetails();
            insResp = new InsuranceDetailsResponse(d.getInsuranceProvider(), d.getPolicyNumber(), d.getGroupNumber());
        }

        EmergencyContactResponse ecResp = null;
        if (patient.getEmergencyContact() != null) {
            EmergencyContact ec = patient.getEmergencyContact();
            ecResp = new EmergencyContactResponse(ec.getName(), ec.getPhone(), ec.getRelationship());
        }

        return new PatientResponse(
            patient.getId(),
            patient.getEmail(),
            patient.getPhone(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getDateOfBirth(),
            patient.getGender(),
            addrResp,
            insResp,
            patient.getAllergies() != null ? List.of(patient.getAllergies()) : List.of(),
            patient.getChronicConditions() != null ? List.of(patient.getChronicConditions()) : List.of(),
            ecResp,
            patient.getCreatedAt()
        );
    }

    private AddressResponse toAddressResponse(Patient patient) {
        Address addr = patient.getAddress();
        return new AddressResponse(
            patient.getId(),
            null, // label — not stored on Patient
            patient.getAddressStreet(),
            patient.getAddressHouseNumber(),
            patient.getAddressApartmentNumber(),
            patient.getAddressCity(),
            patient.getAddressPostalCode(),
            patient.getAddressCountry(),
            addr != null && addr.getLatitude() != null ? addr.getLatitude().doubleValue() : null,
            addr != null && addr.getLongitude() != null ? addr.getLongitude().doubleValue() : null,
            patient.getAddressInstructions(),
            false // isDefault — not on Patient entity
        );
    }
}
