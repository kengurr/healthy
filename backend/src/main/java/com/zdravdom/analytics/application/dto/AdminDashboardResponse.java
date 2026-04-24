package com.zdravdom.analytics.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dashboard KPI response for admin operations screen.
 */
public record AdminDashboardResponse(
    long totalPatients,
    long totalProviders,
    long activeProviders,
    long totalBookings,
    long activeBookings,
    long bookingsToday,
    long bookingsThisWeek,
    BigDecimal revenueToday,
    BigDecimal revenueThisWeek,
    BigDecimal revenueThisMonth,
    long pendingVerifications,
    long openEscalations,
    LocalDateTime generatedAt
) {}