package com.zdravdom.notification.application.service;

import com.zdravdom.notification.application.dto.*;
import com.zdravdom.notification.domain.Notification;
import com.zdravdom.notification.domain.Platform;
import com.zdravdom.notification.domain.PushToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for push notification management.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Transactional
    public void registerPushToken(Long userId, String token, Platform platform) {
        log.info("Registering push token for user: {}, platform: {}", userId, platform);
        // In production, save to database and register with FCM/APNs
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        log.info("Fetching notifications for user: {}", userId);

        List<NotificationResponse> notifications = List.of(
            new NotificationResponse(
                1L, "Booking Confirmed", "Your home nursing visit is confirmed for tomorrow at 9:00",
                Map.of("bookingId", "1"), false, LocalDateTime.now().minusHours(1)
            ),
            new NotificationResponse(
                2L, "Visit Reminder", "Don't forget your physiotherapy session in 2 hours",
                Map.of("visitId", "2"), false, LocalDateTime.now().minusHours(2)
            ),
            new NotificationResponse(
                3L, "Report Ready", "Your visit report from Dr. Horvat is ready to view",
                Map.of("visitId", "3", "reportUrl", "https://..."), true, LocalDateTime.now().minusDays(1)
            )
        );

        return new NotificationListResponse(notifications, page, size, notifications.size(), 1);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        log.info("Marking notification {} as read for user: {}", notificationId, userId);
        return new NotificationResponse(
            notificationId,
            "Booking Confirmed",
            "Your home nursing visit is confirmed for tomorrow at 9:00",
            Map.of("bookingId", "1"),
            true,
            LocalDateTime.now().minusHours(1)
        );
    }
}