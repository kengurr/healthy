package com.zdravdom.billing.adapters.out.stripe;

import java.math.BigDecimal;

/**
 * Stripe gateway interface — abstracts Stripe SDK behind a testable boundary.
 * Allows real Stripe calls in prod and mock implementations in tests/dev.
 */
public interface StripeGateway {

    /**
     * Create a Stripe PaymentIntent for a booking.
     *
     * @param bookingId internal booking ID (used as idempotency key prefix)
     * @param amount   payment amount
     * @param currency three-letter ISO currency code (e.g. "EUR")
     * @return created payment intent with id, client secret, and status
     */
    PaymentIntent createPaymentIntent(Long bookingId, BigDecimal amount, String currency);

    /**
     * Create a Stripe refund for an existing payment intent.
     *
     * @param paymentIntentId Stripe payment intent ID
     * @param amount          refund amount (null = full refund)
     * @return refund result with id and status
     */
    RefundResult createRefund(String paymentIntentId, BigDecimal amount);

    /**
     * Retrieve an existing PaymentIntent by its Stripe ID.
     *
     * @param paymentIntentId Stripe payment intent ID
     * @return payment intent with current status
     */
    PaymentIntent retrievePaymentIntent(String paymentIntentId);

    // ─── Stripe value objects ────────────────────────────────────────────────

    record PaymentIntent(String id, String clientSecret, String status) {}

    record RefundResult(String id, String status) {}
}