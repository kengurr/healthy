package com.zdravdom.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create booking request DTO.
 */
public record CreateBookingRequest(
    @NotNull Long serviceId,
    Long packageId,
    @NotNull Long addressId,
    @NotNull LocalDate date,
    @NotBlank String timeSlot,
    Long providerId,
    String notes
) {}