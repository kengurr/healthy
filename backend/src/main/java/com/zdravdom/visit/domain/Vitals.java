package com.zdravdom.visit.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vital signs recorded during a visit.
 * Maps to visit.vitals table.
 */
@Entity
@Table(name = "vitals", schema = "visit")
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

    // Default constructor for JPA
    public Vitals() {}

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }

    @Transient
    public boolean isAbnormal() {
        if (heartRate != null && (heartRate < 40 || heartRate > 150)) return true;
        if (o2Saturation != null && o2Saturation < 90) return true;
        if (temperature != null && (temperature.compareTo(BigDecimal.valueOf(35.0)) < 0 || temperature.compareTo(BigDecimal.valueOf(39.5)) > 0)) return true;
        return false;
    }

    // Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public String getBloodPressure() { return bloodPressure; }
    public Integer getHeartRate() { return heartRate; }
    public BigDecimal getTemperature() { return temperature; }
    public Integer getO2Saturation() { return o2Saturation; }
    public Integer getRespiratoryRate() { return respiratoryRate; }
    public BigDecimal getBloodGlucose() { return bloodGlucose; }
    public BigDecimal getWeight() { return weight; }
    public String getNotes() { return notes; }
    public LocalDateTime getRecordedAt() { return recordedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public void setO2Saturation(Integer o2Saturation) { this.o2Saturation = o2Saturation; }
    public void setRespiratoryRate(Integer respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    public void setBloodGlucose(BigDecimal bloodGlucose) { this.bloodGlucose = bloodGlucose; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
