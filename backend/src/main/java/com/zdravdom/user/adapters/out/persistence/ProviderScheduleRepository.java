package com.zdravdom.user.adapters.out.persistence;

import com.zdravdom.user.domain.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for ProviderSchedule entities (weekly availability slots and blocked dates).
 */
@Repository
public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, Long> {

    List<ProviderSchedule> findByProviderId(Long providerId);

    Optional<ProviderSchedule> findByProviderIdAndDayOfWeekAndStartTimeAndEndTime(
        Long providerId,
        ProviderSchedule.DayOfWeek dayOfWeek,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime
    );

    List<ProviderSchedule> findByProviderIdAndIsBlockedTrue(Long providerId);

    Optional<ProviderSchedule> findByProviderIdAndBlockedDate(Long providerId, LocalDate blockedDate);

    void deleteByProviderIdAndIsBlockedFalse(Long providerId);
}
