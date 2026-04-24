package com.zdravdom.matching.adapters.out.persistence;

import com.zdravdom.matching.domain.SlotLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * JPA repository for SlotLock DB fallback.
 */
@Repository
public interface SlotLockRepository extends JpaRepository<SlotLock, Long> {

    Optional<SlotLock> findByProviderIdAndSlotDateAndSlotTimeAndReleasedAtIsNull(
        Long providerId, LocalDate slotDate, LocalTime slotTime);

    @Query("""
        SELECT s FROM SlotLock s
        WHERE s.providerId = :providerId
        AND s.slotDate = :slotDate
        AND s.slotTime = :slotTime
        AND s.releasedAt IS NULL
        AND s.expiresAt > CURRENT_TIMESTAMP
        """)
    Optional<SlotLock> findActiveLock(
        @Param("providerId") Long providerId,
        @Param("slotDate") LocalDate slotDate,
        @Param("slotTime") LocalTime slotTime);

    @Modifying
    @Query("""
        UPDATE SlotLock s SET s.releasedAt = CURRENT_TIMESTAMP
        WHERE s.providerId = :providerId
        AND s.slotDate = :slotDate
        AND s.slotTime = :slotTime
        AND s.releasedAt IS NULL
        """)
    int releaseByProviderAndDateAndTime(
        @Param("providerId") Long providerId,
        @Param("slotDate") LocalDate slotDate,
        @Param("slotTime") LocalTime slotTime);

    @Modifying
    @Query("DELETE FROM SlotLock s WHERE s.expiresAt < CURRENT_TIMESTAMP AND s.releasedAt IS NULL")
    int deleteExpiredLocks();
}
