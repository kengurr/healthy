package com.zdravdom.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Refund request DTO.
 */
public record RefundRequest(
    @NotNull UUID bookingId,
    @NotNull BigDecimal amount,
    String reason
) {}