package com.zdravdom.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Refund request DTO.
 */
public record RefundRequest(
    @NotNull Long bookingId,
    @NotNull BigDecimal amount,
    String reason
) {}