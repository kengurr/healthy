package com.zdravdom.notification.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification entity. Maps to notification.notifications table.
 */
@Entity
@Table(name = "notifications", schema = "notification")
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

    // Default constructor for JPA
    public Notification() {}

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserType() { return userType; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getData() { return data; }
    public boolean isRead() { return read; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getReadAt() { return readAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setData(String data) { this.data = data; }
    public void setRead(boolean read) { this.read = read; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
