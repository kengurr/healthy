package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Visit rating submitted by a patient after a completed visit.
 * Maps to visit.visit_ratings table.
 *
 * PRODUCTION: review column is plain TEXT — no PII encryption currently (contains patient-written text).
 *             If reviews may contain health info, encrypt at rest.
 */
@Entity
@Table(name = "visit_ratings", schema = "visit")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /** Factory method for application-layer instantiation. */
    public static VisitRating create() {
        return new VisitRating();
    }
}
