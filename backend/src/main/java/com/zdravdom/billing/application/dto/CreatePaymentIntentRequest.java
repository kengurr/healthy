package com.zdravdom.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Create payment intent request DTO.
 */
public record CreatePaymentIntentRequest(
    @NotNull Long bookingId,
    List<String> allowedNetworks
) {}