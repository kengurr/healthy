package com.zdravdom.billing.config;

import com.zdravdom.billing.adapters.out.stripe.MockStripeGateway;
import com.zdravdom.billing.adapters.out.stripe.RealStripeGateway;
import com.zdravdom.billing.adapters.out.stripe.StripeGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Stripe configuration — selects real or mock gateway based on environment.
 *
 * When {@code stripe.secret-key} is set (prod/staging): creates RealStripeGateway.
 * Otherwise: falls back to MockStripeGateway for dev without Stripe credentials.
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Bean
    public StripeGateway stripeGateway() {
        if (secretKey != null && !secretKey.isBlank()) {
            return new RealStripeGateway(secretKey);
        }
        return new MockStripeGateway();
    }
}