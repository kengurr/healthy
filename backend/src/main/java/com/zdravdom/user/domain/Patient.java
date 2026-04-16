package com.zdravdom.user.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient profile information.
 * Contains personal details, health information, and emergency contacts.
 */
public record Patient(
    Long id,
    String email,
    String phone,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    Gender gender,
    InsuranceDetails insuranceDetails,
    java.util.List<String> allergies,
    java.util.List<String> chronicConditions,
    EmergencyContact emergencyContact,
    Address address,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean verified,
    boolean active
) {
    public Patient {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (allergies == null) allergies = java.util.Collections.emptyList();
        if (chronicConditions == null) chronicConditions = java.util.Collections.emptyList();
    }

    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    public record InsuranceDetails(
        String insuranceProvider,
        String policyNumber,
        String groupNumber
    ) {}

    public record EmergencyContact(
        String name,
        String phone,
        String relationship
    ) {}
}
