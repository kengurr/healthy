package com.zdravdom.user.application.dto;

import com.zdravdom.user.domain.Provider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provider item in the admin verification queue.
 */
public record ProviderVerificationItem(
    Long id,
    String firstName,
    String lastName,
    String email,
    Provider.Profession profession,
    Provider.Specialty specialty,
    Provider.ProviderStatus status,
    List<String> documentUrls,
    LocalDateTime createdAt
) {}