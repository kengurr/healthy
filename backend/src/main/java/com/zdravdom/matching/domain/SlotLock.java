package com.zdravdom.matching.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DB-backed slot lock fallback — used when Redis is unavailable.
 * Maps to matching.slot_locks table.
 *
 * PRODUCTION: Missing composite index on (provider_id, slot_date, slot_time) — used in
 *             findActiveLock(providerId, date, time) which is called on every booking attempt.
 * PRODUCTION: Missing index on expires_at — used in deleteExpiredLocks() cleanup query.
 */
@Entity
@Table(name = "slot_locks", schema = "matching")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_time", nullable = false)
    private LocalTime slotTime;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @PrePersist
    protected void onCreate() {
        if (lockedAt == null) lockedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return releasedAt == null && !isExpired();
    }

    /** Factory method for application-layer instantiation. */
    public static SlotLock create() {
        return new SlotLock();
    }

    /** Generates the Redis lock key for a slot. */
    public static String lockKey(Long providerId, LocalDate date, LocalTime startTime) {
        return String.format("slot:provider:%d:date:%s:time:%s", providerId, date, startTime);
    }
}
