package com.zdravdom.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notification entity. Maps to notification.notifications table.
 *
 * PRODUCTION: Missing index on user_id — used in getNotifications() queries.
 * PRODUCTION: Missing index on (user_id, sent_at) — used in "recent notifications" query pattern.
 * PRODUCTION: Missing index on type — used in notification filtering by type.
 * PRODUCTION: Consider partitioning on user_id or date for large notification tables.
 */
@Entity
@Table(name = "notifications", schema = "notification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "JSONB")
    private String data;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
