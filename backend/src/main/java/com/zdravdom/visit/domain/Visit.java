package com.zdravdom.visit.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Visit entity representing a completed or in-progress home healthcare visit.
 * Contains clinical data, vitals, procedures, and recommendations.
 */
public record Visit(
    Long id,
    Long bookingId,
    Long providerId,
    Long patientId,
    Vitals vitals,
    String clinicalNotes,
    List<String> proceduresPerformed,
    List<String> photos,
    List<String> recommendations,
    String patientSignature,
    VisitStatus status,
    String reportUrl,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public enum VisitStatus {
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ESCALATED
    }

    public Visit {
        if (proceduresPerformed == null) proceduresPerformed = List.of();
        if (photos == null) photos = List.of();
        if (recommendations == null) recommendations = List.of();
        if (status == null) status = VisitStatus.IN_PROGRESS;
    }

    public boolean isInProgress() {
        return status == VisitStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == VisitStatus.COMPLETED;
    }

    public boolean needsEscalation() {
        return status == VisitStatus.ESCALATED;
    }

    public Visit complete() {
        return new Visit(
            id, bookingId, providerId, patientId, vitals,
            clinicalNotes, proceduresPerformed, photos, recommendations,
            patientSignature, VisitStatus.COMPLETED, reportUrl,
            startedAt, LocalDateTime.now(), createdAt, LocalDateTime.now()
        );
    }

    public Visit escalate(Escalation escalation) {
        return new Visit(
            id, bookingId, providerId, patientId, vitals,
            clinicalNotes, proceduresPerformed, photos, recommendations,
            patientSignature, VisitStatus.ESCALATED, reportUrl,
            startedAt, completedAt, createdAt, LocalDateTime.now()
        );
    }
}
