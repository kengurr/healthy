package com.zdravdom.billing.application.dto;

/**
 * Payment intent response DTO with Stripe client secret.
 */
public record PaymentIntentResponse(
    String clientSecret,
    String paymentIntentId
) {}