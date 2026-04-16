package com.zdravdom.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Create payment intent request DTO.
 */
public record CreatePaymentIntentRequest(
    @NotNull UUID bookingId
) {}