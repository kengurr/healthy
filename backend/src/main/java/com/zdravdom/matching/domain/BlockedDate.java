package com.zdravdom.matching.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Blocked date for a provider (vacation, unavailability, etc.).
 * Maps to matching.blocked_dates table.
 */
@Entity
@Table(name = "blocked_dates", schema = "matching")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    @Column(length = 100)
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static BlockedDate create() {
        return new BlockedDate();
    }
}
