package com.zdravdom.billing.adapters.out.stripe;

import com.zdravdom.billing.adapters.out.stripe.StripeGatewayException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock Stripe gateway for development and testing.
 * Simulates Stripe responses without making real API calls.
 * Stores state in memory so confirm/refund lookups work across calls.
 */
@Component
public class MockStripeGateway implements StripeGateway {

    private static final Logger log = LoggerFactory.getLogger(MockStripeGateway.class);

    private final Map<String, PaymentIntent> paymentIntents = new ConcurrentHashMap<>();
    private final Map<String, RefundResult> refunds = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

    @Value("${stripe.mock-failure:false}")
    private boolean simulateFailure;

    @Override
    public PaymentIntent createPaymentIntent(Long bookingId, BigDecimal amount, String currency,
        List<String> allowedNetworks) {
        if (simulateFailure) {
            throw new StripeGatewayException("Stripe API unavailable (mock failure)", null);
        }

        String id = "pi_mock_" + counter.getAndIncrement();
        String clientSecret = id + "_secret_" + counter.getAndIncrement();
        String status = "requires_payment_method";

        PaymentIntent intent = new PaymentIntent(id, clientSecret, status);
        paymentIntents.put(id, intent);

        log.info("[MOCK] Created PaymentIntent {} for booking {} (amount={} {}, networks={})",
            id, bookingId, amount, currency, allowedNetworks);
        return intent;
    }

    @Override
    public RefundResult createRefund(String paymentIntentId, BigDecimal amount) {
        if (simulateFailure) {
            throw new StripeGatewayException("Stripe API unavailable (mock failure)", null);
        }

        PaymentIntent stored = paymentIntents.get(paymentIntentId);
        if (stored == null) {
            throw new ValidationException("Payment intent not found: " + paymentIntentId);
        }

        String refundId = "re_mock_" + counter.getAndIncrement();
        RefundResult result = new RefundResult(refundId, "succeeded");
        refunds.put(refundId, result);

        // Update the payment intent status to reflect refund
        paymentIntents.put(paymentIntentId, new PaymentIntent(
            stored.id(), stored.clientSecret(), "refunded"));

        log.info("[MOCK] Created Refund {} for PaymentIntent {}", refundId, paymentIntentId);
        return result;
    }

    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        PaymentIntent stored = paymentIntents.get(paymentIntentId);
        if (stored == null) {
            throw new ValidationException("Payment intent not found: " + paymentIntentId);
        }
        return stored;
    }

    public void reset() {
        paymentIntents.clear();
        refunds.clear();
    }

    public void setSimulateFailure(boolean simulate) {
        this.simulateFailure = simulate;
    }
}