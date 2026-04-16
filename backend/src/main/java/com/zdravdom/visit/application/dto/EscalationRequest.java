package com.zdravdom.visit.application.dto;

import com.zdravdom.visit.domain.Escalation.UrgencyType;
import jakarta.validation.constraints.NotNull;

/**
 * Escalation request DTO.
 */
public record EscalationRequest(
    @NotNull UrgencyType urgencyType,
    @NotNull String notes,
    GpsLocation gpsLocation
) {
    public record GpsLocation(Double lat, Double lng) {}
}