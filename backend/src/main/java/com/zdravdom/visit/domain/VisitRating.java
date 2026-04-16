package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Visit rating submitted by a patient after a completed visit.
 * Maps to visit.visit_ratings table.
 */
@Entity
@Table(name = "visit_ratings", schema = "visit")
public class VisitRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false, unique = true)
    private Long visitId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public VisitRating() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public Long getPatientId() { return patientId; }
    public Long getProviderId() { return providerId; }
    public Integer getRating() { return rating; }
    public String getReview() { return review; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setReview(String review) { this.review = review; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
