package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vital signs recorded during a visit.
 * Maps to visit.vitals table.
 *
 * PRODUCTION: Missing soft-delete — vitals are health data, 10-year retention required.
 *             No active flag currently — add deleted_at column for GDPR compliance.
 * PRODUCTION: Missing index on visit_id — used in getVisitPdf() and completeVisit() queries.
 * PRODUCTION: Missing index on recorded_at — used in time-range queries for analytics.
 */
@Entity
@Table(name = "vitals", schema = "visit")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vitals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "o2_saturation")
    private Integer o2Saturation;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "blood_glucose", precision = 5, scale = 1)
    private BigDecimal bloodGlucose;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }

    /** Factory method for application-layer instantiation. */
    public static Vitals create() {
        return new Vitals();
    }

    @Transient
    public boolean isAbnormal() {
        if (heartRate != null && (heartRate < 40 || heartRate > 150)) return true;
        if (o2Saturation != null && o2Saturation < 90) return true;
        if (temperature != null && (temperature.compareTo(BigDecimal.valueOf(35.0)) < 0 || temperature.compareTo(BigDecimal.valueOf(39.5)) > 0)) return true;
        return false;
    }
}
