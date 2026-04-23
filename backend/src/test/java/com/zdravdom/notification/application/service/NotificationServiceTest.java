package com.zdravdom.notification.application.service;

import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.global.testing.TestReflectionUtil;
import com.zdravdom.notification.adapters.out.persistence.NotificationRepository;
import com.zdravdom.notification.adapters.out.persistence.PushTokenRepository;
import com.zdravdom.notification.application.dto.NotificationListResponse;
import com.zdravdom.notification.application.dto.NotificationResponse;
import com.zdravdom.notification.application.dto.RegisterPushTokenRequest;
import com.zdravdom.notification.domain.Notification;
import com.zdravdom.notification.domain.PushToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private PushTokenRepository pushTokenRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, pushTokenRepository);
    }

    // ─── registerPushToken ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerPushToken()")
    class RegisterPushToken {

        @Test
        @DisplayName("registers new token and deactivates existing one")
        void registersNewToken() {
            when(pushTokenRepository.findByUserIdAndPlatform(1L, PushToken.Platform.ANDROID))
                .thenReturn(Optional.empty());
            when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(i -> {
                PushToken t = i.getArgument(0);
                t.setId(1L);
                return t;
            });

            RegisterPushTokenRequest request = new RegisterPushTokenRequest("fcm-token-abc", PushToken.Platform.ANDROID);
            notificationService.registerPushToken(1L, request);

            ArgumentCaptor<PushToken> tokenCaptor = ArgumentCaptor.forClass(PushToken.class);
            verify(pushTokenRepository).save(tokenCaptor.capture());

            PushToken saved = tokenCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getToken()).isEqualTo("fcm-token-abc");
            assertThat(saved.getPlatform()).isEqualTo(PushToken.Platform.ANDROID);
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("deactivates existing token before registering new one")
        void deactivatesExistingToken() {
            PushToken existing = PushToken.create();
            existing.setId(5L);
            existing.setUserId(1L);
            existing.setToken("old-token");
            existing.setPlatform(PushToken.Platform.IOS);
            existing.setActive(true);

            when(pushTokenRepository.findByUserIdAndPlatform(1L, PushToken.Platform.IOS))
                .thenReturn(Optional.of(existing));
            when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(i -> i.getArgument(0));

            RegisterPushTokenRequest request = new RegisterPushTokenRequest("new-token", PushToken.Platform.IOS);
            notificationService.registerPushToken(1L, request);

            assertThat(existing.isActive()).isFalse();
            verify(pushTokenRepository, times(2)).save(any(PushToken.class));
        }
    }

    // ─── getNotifications ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getNotifications()")
    class GetNotifications {

        @Test
        @DisplayName("returns paginated notification list")
        void returnsNotificationList() {
            Notification n1 = createNotification(1L, 1L, "Title 1", "Msg 1", false);
            Notification n2 = createNotification(2L, 1L, "Title 2", "Msg 2", true);
            when(notificationRepository.findByUserIdOrderBySentAtDesc(1L))
                .thenReturn(List.of(n1, n2));

            NotificationListResponse response = notificationService.getNotifications(1L, 0, 10);

            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).title()).isEqualTo("Title 1");
            assertThat(response.content().get(1).title()).isEqualTo("Title 2");
        }

        @Test
        @DisplayName("returns empty list when no notifications")
        void returnsEmptyList() {
            when(notificationRepository.findByUserIdOrderBySentAtDesc(1L))
                .thenReturn(List.of());

            NotificationListResponse response = notificationService.getNotifications(1L, 0, 10);

            assertThat(response.content()).isEmpty();
        }
    }

    // ─── markAsRead ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("marks notification as read")
        void marksAsRead() {
            Notification notification = createNotification(1L, 1L, "Title", "Msg", false);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            NotificationResponse response = notificationService.markAsRead(1L, 1L);

            assertThat(response.read()).isTrue();
        }

        @Test
        @DisplayName("throws when notification not found")
        void throwsWhenNotFound() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws when user does not own notification")
        void throwsWhenNotOwner() {
            Notification notification = createNotification(1L, 99L, "Title", "Msg", false);
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Not authorized");
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Notification createNotification(Long id, Long userId, String title, String message, boolean read) {
        Notification n = TestReflectionUtil.newInstance(Notification.class);
        n.setId(id);
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setSentAt(LocalDateTime.now());
        if (read) n.markAsRead();
        return n;
    }
}
