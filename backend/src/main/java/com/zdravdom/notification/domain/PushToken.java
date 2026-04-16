package com.zdravdom.notification.domain;

import java.time.LocalDateTime;

/**
 * Push token entity for device notifications.
 */
public record PushToken(
    Long id,
    Long userId,
    String token,
    Platform platform,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean active
) {
    public PushToken {
        if (active) active = true;
    }
}