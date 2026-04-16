package com.zdravdom.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication token for tracking JWT refresh tokens.
 */
public record AuthToken(
    UUID id,
    UUID userId,
    String refreshToken,
    String deviceInfo,
    String ipAddress,
    Instant issuedAt,
    Instant expiresAt,
    boolean revoked
) {
    public AuthToken {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or blank");
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
