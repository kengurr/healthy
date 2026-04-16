package com.zdravdom.visit.domain;

import java.time.LocalDateTime;

/**
 * Escalation record for urgent situations encountered during a visit.
 */
public record Escalation(
    Long id,
    Long visitId,
    Long providerId,
    Long patientId,
    UrgencyType urgencyType,
    String description,
    String actionTaken,
    String resolution,
    EscalationStatus status,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {
    public enum UrgencyType {
        MEDICAL_EMERGENCY,
        SUSPECTED_ABUSE,
        MEDICATION_ERROR,
        PATIENT_DECLINING,
        EQUIPMENT_FAILURE,
        OTHER
    }

    public enum EscalationStatus {
        OPEN,
        IN_REVIEW,
        RESOLVED,
        ESCALATED_TO_EMERGENCY_SERVICES
    }

    public Escalation {
        if (urgencyType == null) {
            throw new IllegalArgumentException("Urgency type cannot be null");
        }
        if (status == null) status = EscalationStatus.OPEN;
    }

    public boolean isOpen() {
        return status == EscalationStatus.OPEN;
    }

    public boolean requiresEmergencyServices() {
        return urgencyType == UrgencyType.MEDICAL_EMERGENCY;
    }
}
