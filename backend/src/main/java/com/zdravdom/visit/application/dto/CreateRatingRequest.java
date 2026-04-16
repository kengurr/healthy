package com.zdravdom.visit.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Create rating request DTO.
 */
public record CreateRatingRequest(
    @Min(1) @Max(5) int rating,
    String review
) {}