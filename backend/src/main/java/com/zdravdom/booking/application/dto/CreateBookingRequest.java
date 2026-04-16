package com.zdravdom.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Create booking request DTO.
 */
public record CreateBookingRequest(
    @NotNull UUID serviceId,
    UUID packageId,
    @NotNull UUID addressId,
    @NotNull LocalDate date,
    @NotBlank String timeSlot,
    UUID providerId,
    String notes
) {}