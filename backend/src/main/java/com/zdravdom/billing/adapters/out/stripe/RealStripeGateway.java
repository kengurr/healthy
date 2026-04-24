package com.zdravdom.billing.adapters.out.stripe;

import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Real Stripe gateway — wraps Stripe Java SDK calls.
 * Active when {@code stripe.secret-key} is set in configuration.
 */
@Component
public class RealStripeGateway implements StripeGateway {

    private static final Logger log = LoggerFactory.getLogger(RealStripeGateway.class);

    private final String secretKey;

    public RealStripeGateway(@Value("${stripe.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("Stripe secret key is not configured — RealStripeGateway will fail. " +
                "Set stripe.secret-key in application.yml or use MockStripeGateway for development.");
        } else {
            com.stripe.Stripe.apiKey = secretKey;
        }
    }

    @Override
    public PaymentIntent createPaymentIntent(Long bookingId, BigDecimal amount, String currency) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toStripeAmount(amount))
                .setCurrency(currency.toLowerCase())
                .putExtraParam("metadata", Map.of(
                    "booking_id", String.valueOf(bookingId),
                    "platform", "zdravdom"
                ))
                .build();

            var intent = com.stripe.model.PaymentIntent.create(params);

            log.info("Created Stripe PaymentIntent {} for booking {}", intent.getId(), bookingId);
            return new PaymentIntent(intent.getId(), intent.getClientSecret(), intent.getStatus());

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed for booking {}: {}", bookingId, e.getMessage());
            throw new StripeGatewayException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult createRefund(String paymentIntentId, BigDecimal amount) {
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);

            if (amount != null) {
                builder.setAmount(toStripeAmount(amount));
            }

            var refund = com.stripe.model.Refund.create(builder.build());

            log.info("Created Stripe Refund {} for PaymentIntent {}", refund.getId(), paymentIntentId);
            return new RefundResult(refund.getId(), refund.getStatus());

        } catch (StripeException e) {
            log.error("Stripe Refund creation failed for PI {}: {}", paymentIntentId, e.getMessage());
            throw new StripeGatewayException("Failed to create refund: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            var intent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            return new PaymentIntent(intent.getId(), intent.getClientSecret(), intent.getStatus());
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent retrieval failed for {}: {}", paymentIntentId, e.getMessage());
            throw new StripeGatewayException("Failed to retrieve payment intent: " + e.getMessage(), e);
        }
    }

    /** Convert decimal amount (e.g., 45.99) to Stripe's integer cents (e.g., 4599). */
    private static Long toStripeAmount(BigDecimal amount) {
        return amount.movePointRight(2).longValue();
    }
}