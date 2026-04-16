package com.zdravdom.notification.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification entity.
 */
public record Notification(
    Long id,
    Long userId,
    String title,
    String body,
    Map<String, Object> data,
    boolean read,
    LocalDateTime createdAt
) {
    public Notification {
        if (read) read = false;
    }
}