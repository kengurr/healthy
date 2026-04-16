package com.zdravdom.matching.application.dto;

import com.zdravdom.auth.domain.Role;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.Profession;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.user.domain.Provider.Specialty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provider summary for matching results.
 */
public record ProviderSummary(
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
    Double distanceKm,
    LocalDateTime createdAt
) {}