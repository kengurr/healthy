package com.zdravdom.user.application.dto;

import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.ProviderStatus;

import java.util.List;

/**
 * Provider availability response DTO.
 */
public record ProviderAvailabilityResponse(
    List<WeeklyScheduleItem> weeklySchedule,
    List<java.time.LocalDate> blockedDates
) {
    public record WeeklyScheduleItem(
        UpdateAvailabilityRequest.DayOfWeek day,
        String startTime,
        String endTime
    ) {}
}