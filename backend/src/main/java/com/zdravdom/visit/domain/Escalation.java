package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Escalation for urgent situations encountered during a visit.
 * Maps to visit.escalations table.
 *
 * PRODUCTION: Missing index on visit_id — used in escalation lookups.
 * PRODUCTION: Missing index on provider_id — used in provider-level escalation reporting.
 * PRODUCTION: Missing index on urgency_type — used in filtering by emergency level.
 */
@Entity
@Table(name = "escalations", schema = "visit")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_type", nullable = false, length = 50)
    private UrgencyType urgencyType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EscalationStatus status = EscalationStatus.OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "gps_lat", columnDefinition = "DOUBLE PRECISION")
    private Double gpsLat;

    @Column(name = "gps_lng", columnDefinition = "DOUBLE PRECISION")
    private Double gpsLng;

    @ElementCollection
    @CollectionTable(name = "visit.escalation_notified_users", schema = "visit", joinColumns = @JoinColumn(name = "escalation_id"))
    @Column(name = "notified_user")
    private List<String> notifiedUsers;

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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (urgencyType == null) urgencyType = UrgencyType.OTHER;
        if (status == null) status = EscalationStatus.OPEN;
    }

    public boolean isOpen() {
        return status == EscalationStatus.OPEN;
    }

    public boolean requiresEmergencyServices() {
        return urgencyType == UrgencyType.MEDICAL_EMERGENCY;
    }

    /** Factory method for application-layer instantiation. */
    public static Escalation create() {
        return new Escalation();
    }
}
