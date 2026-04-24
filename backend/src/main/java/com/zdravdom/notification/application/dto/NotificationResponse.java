package com.zdravdom.notification.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification response DTO.
 */
public record NotificationResponse(
    Long id,
    String title,
    String body,
    Map<String, Object> data,
    boolean read,
    LocalDateTime createdAt
) {}