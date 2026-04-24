package com.zdravdom.visit.application.dto;

import com.zdravdom.visit.domain.Escalation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Escalation response for admin view.
 */
public record AdminEscalationResponse(
    Long id,
    Long visitId,
    Long providerId,
    String providerName,
    Long patientId,
    String patientName,
    Escalation.UrgencyType urgencyType,
    Escalation.EscalationStatus status,
    String description,
    String actionTaken,
    String resolution,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt,
    Double gpsLat,
    Double gpsLng,
    List<String> notifiedUsers
) {}