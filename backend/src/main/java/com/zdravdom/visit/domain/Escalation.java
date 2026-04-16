package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Escalation for urgent situations encountered during a visit.
 * Maps to visit.escalations table.
 */
@Entity
@Table(name = "escalations", schema = "visit")
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
    private java.util.List<String> notifiedUsers;

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

    // Default constructor for JPA
    public Escalation() {}

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

    // Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public Long getProviderId() { return providerId; }
    public Long getPatientId() { return patientId; }
    public UrgencyType getUrgencyType() { return urgencyType; }
    public String getDescription() { return description; }
    public String getActionTaken() { return actionTaken; }
    public String getResolution() { return resolution; }
    public EscalationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public Double getGpsLat() { return gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public java.util.List<String> getNotifiedUsers() { return notifiedUsers; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setUrgencyType(UrgencyType urgencyType) { this.urgencyType = urgencyType; }
    public void setDescription(String description) { this.description = description; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public void setStatus(EscalationStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public void setNotifiedUsers(java.util.List<String> notifiedUsers) { this.notifiedUsers = notifiedUsers; }
}
