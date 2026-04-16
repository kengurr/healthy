package com.zdravdom.billing.domain;

import com.zdravdom.booking.domain.Booking.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity for billing module.
 */
public record Payment(
    Long id,
    Long bookingId,
    Long patientId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String stripePaymentIntentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public Payment {
        if (currency == null) currency = "EUR";
        if (status == null) status = PaymentStatus.PENDING;
    }
}