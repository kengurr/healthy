package com.zdravdom.user.application.dto;

import com.zdravdom.user.domain.Provider.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Provider availability request DTO.
 */
public record UpdateAvailabilityRequest(
    List<WeeklyScheduleItem> weeklySchedule,
    List<java.time.LocalDate> blockedDates
) {
    public record WeeklyScheduleItem(
        DayOfWeek day,
        String startTime,
        String endTime
    ) {}

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}