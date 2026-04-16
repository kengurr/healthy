package com.zdravdom.billing.application.service;

import com.zdravdom.billing.application.dto.*;
import com.zdravdom.booking.domain.Booking.PaymentStatus;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for payment management with Stripe integration.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public PaymentIntentResponse createPaymentIntent(Long bookingId, BigDecimal amount) {
        log.info("Creating payment intent for booking: {}, amount: {}", bookingId, amount);

        // In production, integrate with Stripe API:
        // StripePaymentIntent intent = stripeClient.paymentIntents().create(...)

        String mockClientSecret = "pi_mock_" + bookingId + "_secret_" + System.currentTimeMillis();
        String mockPaymentIntentId = "pi_mock_" + System.currentTimeMillis();

        return new PaymentIntentResponse(mockClientSecret, mockPaymentIntentId);
    }

    @Transactional
    public PaymentResponse confirmPayment(String paymentIntentId, String status) {
        log.info("Confirming payment: {}, status: {}", paymentIntentId, status);

        PaymentStatus paymentStatus = "succeeded".equals(status) ?
            PaymentStatus.PAID : PaymentStatus.FAILED;

        return new PaymentResponse(
            System.currentTimeMillis(),
            1L, // mock bookingId
            BigDecimal.valueOf(45.00),
            "EUR",
            paymentStatus,
            paymentIntentId,
            LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public PaymentListResponse getPaymentHistory(Long userId, int page, int size) {
        log.info("Fetching payment history for user: {}", userId);

        List<PaymentResponse> payments = List.of(
            new PaymentResponse(
                1L, 1L, BigDecimal.valueOf(45.00), "EUR",
                PaymentStatus.PAID, "pi_abc123", LocalDateTime.now().minusDays(5)
            ),
            new PaymentResponse(
                2L, 2L, BigDecimal.valueOf(55.00), "EUR",
                PaymentStatus.PENDING, "pi_def456", LocalDateTime.now().minusDays(1)
            )
        );

        return new PaymentListResponse(payments, page, size, payments.size(), 1);
    }

    @Transactional
    public PaymentResponse processRefund(RefundRequest request) {
        log.info("Processing refund for booking: {}, amount: {}",
            request.bookingId(), request.amount());

        // In production, integrate with Stripe refund API
        // StripeRefund refund = stripeClient.refunds().create(...)

        return new PaymentResponse(
            System.currentTimeMillis(),
            request.bookingId(),
            request.amount(),
            "EUR",
            PaymentStatus.REFUNDED,
            "pi_mock_refund_" + System.currentTimeMillis(),
            LocalDateTime.now()
        );
    }
}