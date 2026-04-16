package com.zdravdom.user.application.dto;

import com.zdravdom.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

/**
 * Patient update request DTO.
 */
public record UpdatePatientRequest(
    String firstName,
    String lastName,
    String phone,
    InsuranceDetailsRequest insuranceDetails,
    List<String> allergies,
    List<String> chronicConditions,
    EmergencyContactRequest emergencyContact
) {
    public record InsuranceDetailsRequest(
        String insuranceProvider,
        String policyNumber,
        String groupNumber
    ) {}

    public record EmergencyContactRequest(
        String name,
        String phone,
        String relationship
    ) {}
}