package com.zdravdom.visit.application.dto;

import com.zdravdom.visit.domain.Escalation.UrgencyType;
import com.zdravdom.visit.domain.Vitals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Complete visit request DTO with clinical data.
 */
public record CompleteVisitRequest(
    @NotNull VitalsRequest vitals,
    @NotBlank String clinicalNotes,
    List<String> proceduresPerformed,
    List<String> photos,
    String recommendations,
    String patientSignature
) {
    public record VitalsRequest(
        String bloodPressure,
        Integer heartRate,
        Double temperature,
        Integer o2Saturation,
        Integer respiratoryRate,
        Double bloodGlucose,
        Double weight,
        String notes
    ) {}
}