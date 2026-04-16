package com.zdravdom.user.domain;

import com.zdravdom.auth.domain.Role;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Healthcare provider profile information.
 * Contains professional details, qualifications, and availability.
 */
public record Provider(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    Role role,
    Profession profession,
    Specialty specialty,
    Double rating,
    Integer reviewsCount,
    List<Language> languages,
    Integer yearsOfExperience,
    String bio,
    String photoUrl,
    ProviderStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean verified
) {
    public Provider {
        if (languages == null) languages = List.of();
        if (reviewsCount == null) reviewsCount = 0;
        if (rating == null) rating = 0.0;
    }

    public enum Profession {
        NURSE,
        PHYSIOTHERAPIST,
        DOCTOR,
        CAREGIVER,
        SOCIAL_WORKER
    }

    public enum Specialty {
        GENERAL_CARE,
        WOUND_CARE,
        POST_SURGERY_CARE,
        ELDERLY_CARE,
        PEDIATRIC_CARE,
        CHRONIC_DISEASE_MANAGEMENT,
        REHABILITATION,
        PALLIATIVE_CARE,
        MENTAL_HEALTH
    }

    public enum Language {
        SLOVENIAN,
        ENGLISH,
        GERMAN,
        ITALIAN,
        CROATIAN,
        SERBIAN,
        BOSNIAN,
        HUNGARIAN
    }

    public enum ProviderStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED,
        INACTIVE
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
