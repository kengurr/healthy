package com.zdravdom.visit.domain;

import java.time.LocalDateTime;

/**
 * Vital signs recorded during a visit.
 */
public record Vitals(
    String bloodPressure,
    Integer heartRate,
    Double temperature,
    Integer o2Saturation,
    Integer respiratoryRate,
    Double bloodGlucose,
    Double weight,
    String notes,
    LocalDateTime recordedAt
) {
    public Vitals {
        // Validation handled by caller
    }

    public boolean isAbnormal() {
        // Basic range checks for vital signs
        if (heartRate != null && (heartRate < 40 || heartRate > 150)) return true;
        if (o2Saturation != null && o2Saturation < 90) return true;
        if (temperature != null && (temperature < 35.0 || temperature > 39.5)) return true;
        return false;
    }
}
