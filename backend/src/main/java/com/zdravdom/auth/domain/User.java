package com.zdravdom.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity representing an authenticated user in the system.
 * This is the authentication/identity object separate from profile data.
 */
public record User(
    UUID id,
    String email,
    String passwordHash,
    Role role,
    boolean mfaEnabled,
    boolean accountLocked,
    boolean accountExpired,
    boolean credentialsExpired,
    Instant createdAt,
    Instant updatedAt,
    Instant lastLoginAt
) {
    public User {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
    }

    public boolean isEnabled() {
        return !accountLocked && !accountExpired && !credentialsExpired;
    }

    public boolean isProvider() {
        return role == Role.PROVIDER;
    }

    public boolean isPatient() {
        return role == Role.PATIENT;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SUPERADMIN;
    }
}
