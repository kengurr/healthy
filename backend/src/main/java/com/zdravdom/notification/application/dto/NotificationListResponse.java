package com.zdravdom.notification.application.dto;

import java.util.List;

/**
 * Paginated notification list response.
 */
public record NotificationListResponse(
    List<NotificationResponse> content,
    int page,
    int size,
    long total,
    int totalPages
) {}