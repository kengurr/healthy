package com.zdravdom.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Create payment intent request DTO.
 */
public record CreatePaymentIntentRequest(
    @NotNull Long bookingId
) {}