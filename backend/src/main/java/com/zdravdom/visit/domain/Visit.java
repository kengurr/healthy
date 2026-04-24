package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Visit entity representing a completed or in-progress home healthcare visit.
 * Maps to visit.visits table.
 *
 * PRODUCTION: Missing soft-delete — health data requires 10-year retention.
 *             Implement logical deletion (active flag or deleted_at column) and
 *             anonymization on soft-delete (GDPR Art. 17).
 * PRODUCTION: Missing index on booking_id, provider_id, patient_id, date — used in most queries.
 * PRODUCTION: clinical_notes and patient_signature stored as plain TEXT — encrypt at rest
 *             or use application-level encryption for HIPAA/GDPR compliance.
 */
@Entity
@Table(name = "visits", schema = "visit")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VisitStatus status = VisitStatus.IN_PROGRESS;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @ElementCollection
    @CollectionTable(name = "visit.visit_procedures", schema = "visit", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "procedure")
    private List<String> proceduresPerformed;

    @ElementCollection
    @CollectionTable(name = "visit.visit_photos", schema = "visit", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "photo_url")
    private List<String> photos;

    @ElementCollection
    @CollectionTable(name = "visit.visit_recommendations", schema = "visit", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "recommendation")
    private List<String> recommendations;

    @Column(name = "patient_signature", columnDefinition = "TEXT")
    private String patientSignature;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Column(name = "gps_lat", columnDefinition = "DOUBLE PRECISION")
    private Double gpsLat;

    @Column(name = "gps_lng", columnDefinition = "DOUBLE PRECISION")
    private Double gpsLng;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Factory method for application-layer instantiation (e.g., test mocks, service code). */
    public static Visit create() {
        return new Visit();
    }

    public enum VisitStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ESCALATED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = VisitStatus.IN_PROGRESS;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public void complete() {
        this.status = VisitStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void escalate() {
        this.status = VisitStatus.ESCALATED;
    }
}
