package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Visit entity representing a completed or in-progress home healthcare visit.
 * Maps to visit.visits table.
 */
@Entity
@Table(name = "visits", schema = "visit")
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
    private java.util.List<String> proceduresPerformed;

    @ElementCollection
    @CollectionTable(name = "visit.visit_photos", schema = "visit", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "photo_url")
    private java.util.List<String> photos;

    @ElementCollection
    @CollectionTable(name = "visit.visit_recommendations", schema = "visit", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "recommendation")
    private java.util.List<String> recommendations;

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

    // Vitals stored in separate visit.vitals table — joined on visit_id
    @Transient
    private Vitals vitals;

    public enum VisitStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ESCALATED
    }

    // Default constructor for JPA
    public Visit() {}

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

    // Getters
    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getProviderId() { return providerId; }
    public Long getPatientId() { return patientId; }
    public VisitStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Vitals getVitals() { return vitals; }
    public String getClinicalNotes() { return clinicalNotes; }
    public java.util.List<String> getProceduresPerformed() { return proceduresPerformed; }
    public java.util.List<String> getPhotos() { return photos; }
    public java.util.List<String> getRecommendations() { return recommendations; }
    public String getPatientSignature() { return patientSignature; }
    public String getReportUrl() { return reportUrl; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setStatus(VisitStatus status) { this.status = status; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setVitals(Vitals vitals) { this.vitals = vitals; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
    public void setProceduresPerformed(java.util.List<String> proceduresPerformed) { this.proceduresPerformed = proceduresPerformed; }
    public void setPhotos(java.util.List<String> photos) { this.photos = photos; }
    public void setRecommendations(java.util.List<String> recommendations) { this.recommendations = recommendations; }
    public void setPatientSignature(String patientSignature) { this.patientSignature = patientSignature; }
    public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }
}
