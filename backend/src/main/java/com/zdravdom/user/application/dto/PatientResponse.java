package com.zdravdom.user.application.dto;

import com.zdravdom.user.domain.Patient.Gender;
import com.zdravdom.user.domain.Address;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Patient response DTO for API.
 */
public record PatientResponse(
    Long id,
    String email,
    String phone,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    Gender gender,
    AddressResponse address,
    InsuranceDetailsResponse insuranceDetails,
    List<String> allergies,
    List<String> chronicConditions,
    EmergencyContactResponse emergencyContact,
    LocalDateTime createdAt
) {
    public record AddressResponse(
        Long id,
        String label,
        String street,
        String houseNumber,
        String apartmentNumber,
        String city,
        String postalCode,
        String country,
        Double latitude,
        Double longitude,
        String instructions,
        boolean isDefault
    ) {}

    public record InsuranceDetailsResponse(
        String insuranceProvider,
        String policyNumber,
        String groupNumber
    ) {}

    public record EmergencyContactResponse(
        String name,
        String phone,
        String relationship
    ) {}
}