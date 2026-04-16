package com.zdravdom.notification.application.dto;

import com.zdravdom.notification.domain.Platform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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