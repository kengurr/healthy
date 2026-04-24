package com.zdravdom.billing.adapters.out.stripe;

/**
 * Exception thrown by Stripe gateway operations on failure.
 */
public class StripeGatewayException extends RuntimeException {
    public StripeGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}