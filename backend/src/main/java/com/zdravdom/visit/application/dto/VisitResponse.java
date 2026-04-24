package com.zdravdom.visit.application.dto;

import com.zdravdom.visit.domain.Visit.VisitStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Visit response DTO.
 */
public record VisitResponse(
    Long id,
    Long bookingId,
    Long providerId,
    Long patientId,
    VitalsResponse vitals,
    String clinicalNotes,
    List<String> proceduresPerformed,
    List<String> photos,
    List<String> recommendations,
    String patientSignature,
    VisitStatus status,
    String reportUrl,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {}
