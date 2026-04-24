package com.zdravdom.matching.application.service;

import com.zdravdom.matching.adapters.out.persistence.SlotLockRepository;
import com.zdravdom.matching.domain.SlotLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * Distributed slot locking using Redis (Redisson) with DB fallback.
 * Uses the slot lock pattern: SETNX-like acquire + TTL, release on confirm/cancel.
 */
@Service
public class SlotLockService {

    private static final Logger log = LoggerFactory.getLogger(SlotLockService.class);

    private static final int DEFAULT_LOCK_MINUTES = 5; // PRODUCTION: Lock TTL must be configurable — 5 min may be too short for slow booking flows

    private final RedissonClient redissonClient;
    private final SlotLockRepository slotLockRepository;

    public SlotLockService(RedissonClient redissonClient, SlotLockRepository slotLockRepository) {
        this.redissonClient = redissonClient;
        this.slotLockRepository = slotLockRepository;
    }

    /**
     * Try to acquire a distributed lock on a provider/date/time slot.
     * Uses Redis (Redisson RLock) with DB fallback when Redis is unavailable.
     *
     * @return true if lock acquired, false if slot is already locked
     */
    public boolean tryLock(Long providerId, LocalDate date, LocalTime timeSlot,
                           String idempotencyKey) {
        return tryLock(providerId, date, timeSlot, idempotencyKey, DEFAULT_LOCK_MINUTES);
    }

    /**
     * Try to acquire a distributed lock with custom TTL.
     */
    public boolean tryLock(Long providerId, LocalDate date, LocalTime timeSlot,
                           String idempotencyKey, int lockDurationMinutes) {
        String lockKey = SlotLock.lockKey(providerId, date, timeSlot);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // tryLock returns true if lock acquired, false otherwise
            boolean acquired = lock.tryLock(0, lockDurationMinutes * 60L, TimeUnit.SECONDS);
            if (acquired) {
                // Persist to DB as fallback record
                persistLock(providerId, date, timeSlot, idempotencyKey, lockDurationMinutes);
                log.info("Lock acquired: {} (idempotencyKey={})", lockKey, idempotencyKey);
            } else {
                log.debug("Lock already held: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted: {}", lockKey);
            return tryLockDbFallback(providerId, date, timeSlot, idempotencyKey, lockDurationMinutes);
        } catch (Exception e) {
            // Redis unavailable — fall back to DB
            log.warn("Redis lock failed, using DB fallback: {}", e.getMessage());
            return tryLockDbFallback(providerId, date, timeSlot, idempotencyKey, lockDurationMinutes);
        }
    }

    /**
     * Release a previously acquired slot lock.
     */
    @Transactional
    public void releaseLock(Long providerId, LocalDate date, LocalTime timeSlot) {
        String lockKey = SlotLock.lockKey(providerId, date, timeSlot);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock released via Redis: {}", lockKey);
            }
        } catch (Exception e) {
            log.warn("Redis unlock failed, releasing via DB: {}", e.getMessage());
        }

        // Always release in DB too (idempotent)
        releaseDbLock(providerId, date, timeSlot);
    }

    /**
     * Check if a slot is currently locked.
     */
    public boolean isLocked(Long providerId, LocalDate date, LocalTime timeSlot) {
        String lockKey = SlotLock.lockKey(providerId, date, timeSlot);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.isLocked()) {
                return true;
            }
        } catch (Exception e) {
            log.debug("Redis isLocked check failed: {}", e.getMessage());
        }

        // Fall back to DB check
        return isLockedInDb(providerId, date, timeSlot);
    }

    // ─── DB Fallback helpers ─────────────────────────────────────────────────

    @Transactional
    protected boolean tryLockDbFallback(Long providerId, LocalDate date, LocalTime timeSlot,
                                        String idempotencyKey, int lockDurationMinutes) {
        // Clean up expired locks first
        slotLockRepository.deleteExpiredLocks();

        // Check if already locked
        if (slotLockRepository.findActiveLock(providerId, date, timeSlot).isPresent()) {
            return false;
        }

        SlotLock slotLock = SlotLock.create();
        slotLock.setProviderId(providerId);
        slotLock.setSlotDate(date);
        slotLock.setSlotTime(timeSlot);
        slotLock.setIdempotencyKey(idempotencyKey);
        slotLock.setExpiresAt(LocalDateTime.now().plusMinutes(lockDurationMinutes));
        slotLockRepository.save(slotLock);
        return true;
    }

    @Transactional
    protected void releaseDbLock(Long providerId, LocalDate date, LocalTime timeSlot) {
        slotLockRepository.releaseByProviderAndDateAndTime(providerId, date, timeSlot);
    }

    protected boolean isLockedInDb(Long providerId, LocalDate date, LocalTime timeSlot) {
        return slotLockRepository.findActiveLock(providerId, date, timeSlot).isPresent();
    }

    private void persistLock(Long providerId, LocalDate date, LocalTime timeSlot,
                             String idempotencyKey, int lockDurationMinutes) {
        try {
            SlotLock slotLock = SlotLock.create();
            slotLock.setProviderId(providerId);
            slotLock.setSlotDate(date);
            slotLock.setSlotTime(timeSlot);
            slotLock.setIdempotencyKey(idempotencyKey);
            slotLock.setExpiresAt(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            slotLockRepository.save(slotLock);
        } catch (Exception e) {
            log.warn("Failed to persist slot lock to DB (Redis lock still holds): {}", e.getMessage());
        }
    }
}
