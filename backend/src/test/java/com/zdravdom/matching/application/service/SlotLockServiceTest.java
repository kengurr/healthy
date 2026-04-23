package com.zdravdom.matching.application.service;

import com.zdravdom.matching.adapters.out.persistence.SlotLockRepository;
import com.zdravdom.matching.domain.SlotLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotLockServiceTest {

    @Mock private RedissonClient redissonClient;
    @Mock private SlotLockRepository slotLockRepository;
    @Mock private RLock rLock;

    private SlotLockService slotLockService;

    @BeforeEach
    void setUp() {
        slotLockService = new SlotLockService(redissonClient, slotLockRepository);
    }

    // ─── tryLock via Redis ──────────────────────────────────────────────────

    @Nested
    @DisplayName("tryLock()")
    class TryLock {

        @Test
        @DisplayName("acquires Redis lock and persists to DB when Redis succeeds")
        void acquiresLockAndPersistsToDb() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(0L, 300L, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

            boolean result = slotLockService.tryLock(1L, LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0), "idem-key");

            assertThat(result).isTrue();
            verify(slotLockRepository).save(any(SlotLock.class));
        }

        @Test
        @DisplayName("returns false when Redis lock is already held")
        void returnsFalseWhenAlreadyLocked() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(0L, 300L, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(false);

            boolean result = slotLockService.tryLock(1L, LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0), "idem-key");

            assertThat(result).isFalse();
            verify(slotLockRepository, never()).save(any());
        }

        @Test
        @DisplayName("uses DB fallback when Redis throws exception")
        void usesDbFallbackOnRedisException() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(0L, 300L, java.util.concurrent.TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("Redis unavailable"));
            when(slotLockRepository.findActiveLock(any(), any(), any())).thenReturn(Optional.empty());

            boolean result = slotLockService.tryLock(1L, LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0), "idem-key");

            assertThat(result).isTrue();
            verify(slotLockRepository).save(any(SlotLock.class));
        }

        @Test
        @DisplayName("returns false from DB fallback when slot already locked")
        void returnsFalseFromDbFallbackWhenAlreadyLocked() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(0L, 300L, java.util.concurrent.TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("Redis unavailable"));
            when(slotLockRepository.findActiveLock(any(), any(), any())).thenReturn(Optional.of(SlotLock.create()));

            boolean result = slotLockService.tryLock(1L, LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0), "idem-key");

            assertThat(result).isFalse();
        }
    }

    // ─── releaseLock ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("releaseLock()")
    class ReleaseLock {

        @Test
        @DisplayName("releases Redis lock and DB record")
        void releasesLock() {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            slotLockService.releaseLock(1L, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0));

            verify(rLock).unlock();
            verify(slotLockRepository).releaseByProviderAndDateAndTime(
                eq(1L), eq(LocalDate.of(2026, 5, 1)), eq(LocalTime.of(9, 0)));
        }

        @Test
        @DisplayName("releases DB lock even when Redis unlock fails")
        void releasesDbEvenWhenRedisFails() {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.isHeldByCurrentThread()).thenThrow(new RuntimeException("Redis error"));

            slotLockService.releaseLock(1L, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0));

            verify(slotLockRepository).releaseByProviderAndDateAndTime(
                eq(1L), eq(LocalDate.of(2026, 5, 1)), eq(LocalTime.of(9, 0)));
        }
    }

    // ─── isLocked ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isLocked()")
    class IsLocked {

        @Test
        @DisplayName("returns true when Redis reports lock held")
        void returnsTrueWhenRedisLocked() {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.isLocked()).thenReturn(true);

            boolean result = slotLockService.isLocked(1L, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("falls back to DB when Redis isLocked throws")
        void fallsBackToDbOnRedisError() {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.isLocked()).thenThrow(new RuntimeException("Redis error"));
            when(slotLockRepository.findActiveLock(any(), any(), any())).thenReturn(Optional.of(SlotLock.create()));

            boolean result = slotLockService.isLocked(1L, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when neither Redis nor DB has lock")
        void returnsFalseWhenUnlocked() {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.isLocked()).thenReturn(false);
            when(slotLockRepository.findActiveLock(any(), any(), any())).thenReturn(Optional.empty());

            boolean result = slotLockService.isLocked(1L, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0));

            assertThat(result).isFalse();
        }
    }
}
