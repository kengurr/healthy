package com.zdravdom.visit.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vitals response DTO.
 */
public record VitalsResponse(
    Long id,
    Long visitId,
    String bloodPressure,
    Integer heartRate,
    BigDecimal temperature,
    Integer o2Saturation,
    Integer respiratoryRate,
    BigDecimal bloodGlucose,
    BigDecimal weight,
    String notes,
    LocalDateTime recordedAt
) {}
