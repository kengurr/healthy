package com.zdravdom.billing.application.dto;

import com.zdravdom.booking.domain.Booking.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment response DTO.
 */
public record PaymentResponse(
    Long id,
    Long bookingId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String stripePaymentIntentId,
    LocalDateTime createdAt
) {}