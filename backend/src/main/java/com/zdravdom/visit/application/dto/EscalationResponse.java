package com.zdravdom.visit.application.dto;

import com.zdravdom.visit.domain.Escalation.UrgencyType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Escalation response DTO.
 */
public record EscalationResponse(
    Long id,
    Long visitId,
    UrgencyType urgencyType,
    String notes,
    EscalationRequest.GpsLocation gpsLocation,
    LocalDateTime timestamp,
    List<String> notifiedUsers
) {}