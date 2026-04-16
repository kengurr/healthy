package com.zdravdom.user.application.service;

import com.zdravdom.user.application.dto.*;
import com.zdravdom.user.application.dto.PatientResponse.AddressResponse;
import com.zdravdom.user.application.dto.PatientResponse.EmergencyContactResponse;
import com.zdravdom.user.application.dto.PatientResponse.InsuranceDetailsResponse;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Patient.EmergencyContact;
import com.zdravdom.user.domain.Patient.InsuranceDetails;
import com.zdravdom.user.domain.Address;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for patient profile management.
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    @Transactional
    public PatientResponse getPatientByUserId(UUID userId) {
        // For MVP, create a mock patient. In production, this would query patient by userId
        Patient patient = getOrCreatePatient(userId);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(UUID userId, UpdatePatientRequest request) {
        Patient patient = getOrCreatePatient(userId);

        Patient updated = new Patient(
            patient.id(),
            patient.email(),
            patient.phone(),
            request.firstName() != null ? request.firstName() : patient.firstName(),
            request.lastName() != null ? request.lastName() : patient.lastName(),
            patient.dateOfBirth(),
            patient.gender(),
            request.insuranceDetails() != null ?
                new InsuranceDetails(
                    request.insuranceDetails().insuranceProvider(),
                    request.insuranceDetails().policyNumber(),
                    request.insuranceDetails().groupNumber()
                ) : patient.insuranceDetails(),
            request.allergies() != null ? request.allergies() : patient.allergies(),
            request.chronicConditions() != null ? request.chronicConditions() : patient.chronicConditions(),
            request.emergencyContact() != null ?
                new EmergencyContact(
                    request.emergencyContact().name(),
                    request.emergencyContact().phone(),
                    request.emergencyContact().relationship()
                ) : patient.emergencyContact(),
            patient.address(),
            patient.createdAt(),
            LocalDateTime.now(),
            patient.verified(),
            patient.active()
        );

        log.info("Updated patient profile for userId: {}", userId);
        return toResponse(updated);
    }

    @Transactional
    public List<AddressResponse> getAddresses(UUID userId) {
        // For MVP, return mock addresses. In production, query by patientId
        Patient patient = getOrCreatePatient(userId);
        if (patient.address() != null) {
            return List.of(toAddressResponse(patient.address()));
        }
        return List.of();
    }

    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        Patient patient = getOrCreatePatient(userId);

        Address address = new Address(
            null,
            request.street(),
            request.houseNumber(),
            request.apartmentNumber(),
            request.city(),
            request.postalCode(),
            null,
            request.country() != null ? request.country() : "SI",
            request.latitude() != null ? java.math.BigDecimal.valueOf(request.latitude()) : null,
            request.longitude() != null ? java.math.BigDecimal.valueOf(request.longitude()) : null,
            request.instructions()
        );

        Patient updated = new Patient(
            patient.id(), patient.email(), patient.phone(), patient.firstName(),
            patient.lastName(), patient.dateOfBirth(), patient.gender(),
            patient.insuranceDetails(), patient.allergies(), patient.chronicConditions(),
            patient.emergencyContact(), address, patient.createdAt(),
            LocalDateTime.now(), patient.verified(), patient.active()
        );

        log.info("Added address for patient userId: {}", userId);
        return toAddressResponse(address);
    }

    @Transactional
    public GDPRExportResponse exportGdprData(UUID userId) {
        Patient patient = getOrCreatePatient(userId);
        // In production, this would trigger an async job to generate a ZIP file
        String jobId = UUID.randomUUID().toString();
        log.info("Started GDPR export for userId: {}, jobId: {}", userId, jobId);
        return new GDPRExportResponse(
            UUID.fromString(jobId),
            "PENDING",
            null
        );
    }

    public record GDPRExportResponse(
        UUID jobId,
        String status,
        String downloadUrl
    ) {}

    private Patient getOrCreatePatient(UUID userId) {
        // For MVP: create a mock patient. In production, query by userId
        return new Patient(
            1L,
            "patient@example.com",
            "+38612345678",
            "Janez",
            "Novak",
            java.time.LocalDate.of(1980, 5, 15),
            Patient.Gender.MALE,
            new InsuranceDetails("ZZZS", "123456789", "GRP001"),
            List.of("Penicillin"),
            List.of("Hypertension"),
            new EmergencyContact("Marija Novak", "+38698765432", "Spouse"),
            new Address(
                1L, "Ljubljanska cesta", "10", "5", "Ljubljana", "1000",
                "Central Slovenia", "SI",
                java.math.BigDecimal.valueOf(46.056946),
                java.math.BigDecimal.valueOf(14.505751),
                "Ring bell twice"
            ),
            LocalDateTime.now().minusMonths(6),
            LocalDateTime.now(),
            true,
            true
        );
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
            patient.id(),
            patient.email(),
            patient.phone(),
            patient.firstName(),
            patient.lastName(),
            patient.dateOfBirth(),
            patient.gender(),
            patient.address() != null ? toAddressResponse(patient.address()) : null,
            patient.insuranceDetails() != null ?
                new InsuranceDetailsResponse(
                    patient.insuranceDetails().insuranceProvider(),
                    patient.insuranceDetails().policyNumber(),
                    patient.insuranceDetails().groupNumber()
                ) : null,
            patient.allergies(),
            patient.chronicConditions(),
            patient.emergencyContact() != null ?
                new EmergencyContactResponse(
                    patient.emergencyContact().name(),
                    patient.emergencyContact().phone(),
                    patient.emergencyContact().relationship()
                ) : null,
            patient.createdAt()
        );
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
            address.id(),
            null,
            address.street(),
            address.houseNumber(),
            address.apartmentNumber(),
            address.city(),
            address.postalCode(),
            address.country(),
            address.latitude() != null ? address.latitude().doubleValue() : null,
            address.longitude() != null ? address.longitude().doubleValue() : null,
            address.instructions(),
            false
        );
    }
}