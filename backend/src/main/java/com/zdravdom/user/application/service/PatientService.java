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

import java.math.BigDecimal;
import java.time.LocalDate;
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
    public PatientResponse getPatientByUserId(Long userId) {
        Patient patient = getOrCreatePatient(userId);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(Long userId, UpdatePatientRequest request) {
        Patient patient = getOrCreatePatient(userId);

        patient.setFirstName(request.firstName() != null ? request.firstName() : patient.getFirstName());
        patient.setLastName(request.lastName() != null ? request.lastName() : patient.getLastName());
        if (request.insuranceDetails() != null) {
            InsuranceDetails details = new InsuranceDetails(
                request.insuranceDetails().insuranceProvider(),
                request.insuranceDetails().policyNumber(),
                request.insuranceDetails().groupNumber()
            );
            patient.setInsuranceDetails(details);
        }
        if (request.allergies() != null) {
            patient.setAllergies(request.allergies().toArray(new String[0]));
        }
        if (request.chronicConditions() != null) {
            patient.setChronicConditions(request.chronicConditions().toArray(new String[0]));
        }
        if (request.emergencyContact() != null) {
            EmergencyContact ec = new EmergencyContact(
                request.emergencyContact().name(),
                request.emergencyContact().phone(),
                request.emergencyContact().relationship()
            );
            patient.setEmergencyContact(ec);
        }

        log.info("Updated patient profile for userId: {}", userId);
        return toResponse(patient);
    }

    @Transactional
    public List<AddressResponse> getAddresses(Long userId) {
        Patient patient = getOrCreatePatient(userId);
        if (patient.getAddressStreet() != null) {
            return List.of(toAddressResponse(patient));
        }
        return List.of();
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        Patient patient = getOrCreatePatient(userId);

        patient.setAddressStreet(request.street());
        patient.setAddressHouseNumber(request.houseNumber());
        patient.setAddressApartmentNumber(request.apartmentNumber());
        patient.setAddressCity(request.city());
        patient.setAddressPostalCode(request.postalCode());
        patient.setAddressRegion(null);
        patient.setAddressCountry(request.country() != null ? request.country() : "SI");
        patient.setAddressInstructions(request.instructions());

        log.info("Added address for patient userId: {}", userId);
        return toAddressResponse(patient);
    }

    @Transactional
    public GDPRExportResponse exportGdprData(Long userId) {
        Patient patient = getOrCreatePatient(userId);
        String jobId = UUID.randomUUID().toString();
        log.info("Started GDPR export for userId: {}, jobId: {}", userId, jobId);
        return new GDPRExportResponse(UUID.fromString(jobId), "PENDING", null);
    }

    public record GDPRExportResponse(UUID jobId, String status, String downloadUrl) {}

    private Patient getOrCreatePatient(Long userId) {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setEmail("patient@example.com");
        patient.setPhone("+38612345678");
        patient.setFirstName("Janez");
        patient.setLastName("Novak");
        patient.setDateOfBirth(LocalDate.of(1980, 5, 15));
        patient.setGender(Patient.Gender.MALE);
        InsuranceDetails details = new InsuranceDetails("ZZZS", "123456789", "GRP001");
        patient.setInsuranceDetails(details);
        patient.setAllergies(new String[]{"Penicillin"});
        patient.setChronicConditions(new String[]{"Hypertension"});
        EmergencyContact ec = new EmergencyContact("Marija Novak", "+38698765432", "Spouse");
        patient.setEmergencyContact(ec);
        patient.setAddressStreet("Ljubljanska cesta");
        patient.setAddressHouseNumber("10");
        patient.setAddressApartmentNumber("5");
        patient.setAddressCity("Ljubljana");
        patient.setAddressPostalCode("1000");
        patient.setAddressRegion("Central Slovenia");
        patient.setAddressCountry("SI");
        patient.setAddressInstructions("Ring bell twice");
        patient.setCreatedAt(LocalDateTime.now().minusMonths(6));
        patient.setUpdatedAt(LocalDateTime.now());
        patient.setVerified(true);
        patient.setActive(true);
        return patient;
    }

    private PatientResponse toResponse(Patient patient) {
        Address addr = patient.getAddress();
        AddressResponse addrResp = addr != null && addr.getStreet() != null ? toAddressResponse(patient) : null;
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
        return new AddressResponse(
            1L,
            null,
            patient.getAddressStreet(),
            patient.getAddressHouseNumber(),
            patient.getAddressApartmentNumber(),
            patient.getAddressCity(),
            patient.getAddressPostalCode(),
            patient.getAddressCountry(),
            null,
            null,
            patient.getAddressInstructions(),
            false
        );
    }
}
