package com.zdravdom.matching.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Redis-based slot lock for preventing double-booking during concurrent requests.
 */
public record SlotLock(
    String lockKey,
    Long providerId,
    Long patientId,
    LocalDate date,
    TimeSlot timeSlot,
    String idempotencyKey,
    Instant lockedAt,
    Instant expiresAt
) {
    public record TimeSlot(
        LocalTime startTime,
        LocalTime endTime
    ) {}

    public SlotLock {
        if (lockKey == null || lockKey.isBlank()) {
            throw new IllegalArgumentException("Lock key cannot be null or blank");
        }
        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("Time slot cannot be null");
        }
    }

    public static String generateLockKey(Long providerId, LocalDate date, LocalTime startTime) {
        return String.format("slot:provider:%d:date:%s:time:%s", providerId, date, startTime);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getTtlSeconds() {
        return Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
    }
}
