package com.zdravdom.notification.application.service;

import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.notification.adapters.out.persistence.NotificationRepository;
import com.zdravdom.notification.adapters.out.persistence.PushTokenRepository;
import com.zdravdom.notification.application.dto.NotificationListResponse;
import com.zdravdom.notification.application.dto.NotificationResponse;
import com.zdravdom.notification.application.dto.RegisterPushTokenRequest;
import com.zdravdom.notification.domain.Notification;
import com.zdravdom.notification.domain.PushToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for push notification management.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final PushTokenRepository pushTokenRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                PushTokenRepository pushTokenRepository) {
        this.notificationRepository = notificationRepository;
        this.pushTokenRepository = pushTokenRepository;
    }

    @Transactional
    public void registerPushToken(Long userId, RegisterPushTokenRequest request) {
        log.info("Registering push token for user: {}, platform: {}", userId, request.platform());

        // Deactivate any existing token for same user/platform
        pushTokenRepository.findByUserIdAndPlatform(userId, request.platform())
            .ifPresent(existing -> {
                existing.deactivate();
                pushTokenRepository.save(existing);
            });

        PushToken token = PushToken.create();
        token.setUserId(userId);
        token.setToken(request.token());
        token.setPlatform(request.platform());
        token.setActive(true);
        pushTokenRepository.save(token);

        log.info("Push token registered for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, int page, int size) {
        log.info("Fetching notifications for user: {}", userId);

        List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(userId);

        List<NotificationResponse> content = notifications.stream()
            .map(this::toResponse)
            .toList();

        return new NotificationListResponse(content, page, size, content.size(),
            (int) Math.ceil((double) content.size() / size));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        log.info("Marking notification {} as read for user: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ValidationException("Not authorized to modify this notification");
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    // ─── Response mapping ────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        Map<String, Object> data = null;
        if (n.getData() != null) {
            data = Map.of("data", (Object) n.getData());
        }
        return new NotificationResponse(
            n.getId(),
            n.getTitle(),
            n.getMessage(),
            data,
            n.isRead(),
            n.getSentAt()
        );
    }
}
