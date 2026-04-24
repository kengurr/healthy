package com.zdravdom.billing.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.billing.application.dto.*;
import com.zdravdom.billing.application.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for payment management.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Stripe payments, refunds")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    @Operation(summary = "Create Stripe payment intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        // In production, get amount from booking
        PaymentIntentResponse response = paymentService.createPaymentIntent(
            request.bookingId(), null, request.allowedNetworks());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment (Stripe webhook handler)")
    // PRODUCTION: Must verify Stripe webhook signature (stripe-signature header) — otherwise anyone can fake a payment confirmation
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(
            request.paymentIntentId(), request.status());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Payment history")
    public ResponseEntity<PaymentListResponse> getPaymentHistory(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaymentListResponse response = paymentService.getPaymentHistory(
            principal.userId(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Request a refund")
    public ResponseEntity<PaymentResponse> processRefund(@Valid @RequestBody RefundRequest request) {
        PaymentResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(response);
    }

    public record ConfirmPaymentRequest(String paymentIntentId, String status) {}
}