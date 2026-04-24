package com.zdravdom.billing.application.service;

import com.zdravdom.billing.adapters.out.persistence.PaymentRepository;
import com.zdravdom.billing.adapters.out.stripe.StripeGateway;
import com.zdravdom.billing.application.dto.PaymentListResponse;
import com.zdravdom.billing.application.dto.PaymentResponse;
import com.zdravdom.billing.application.dto.RefundRequest;
import com.zdravdom.billing.domain.Payment;
import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private StripeGateway stripeGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, bookingRepository, stripeGateway);
    }

    // ─── createPaymentIntent ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createPaymentIntent()")
    class CreatePaymentIntent {

        @Test
        @DisplayName("calls Stripe gateway with booking ID and resolves amount from booking")
        void callsStripeGatewayWithCorrectParams() {
            Booking booking = mock(Booking.class);
            when(booking.getPaymentAmount()).thenReturn(BigDecimal.valueOf(85.00));
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

            when(stripeGateway.createPaymentIntent(eq(10L), eq(BigDecimal.valueOf(85.00)), eq("eur"), any()))
                .thenReturn(new StripeGateway.PaymentIntent("pi_123", "cs_123", "requires_payment_method"));

            var response = paymentService.createPaymentIntent(10L, null, null);

            assertThat(response.paymentIntentId()).isEqualTo("pi_123");
            assertThat(response.clientSecret()).isEqualTo("cs_123");
            verify(stripeGateway).createPaymentIntent(eq(10L), eq(BigDecimal.valueOf(85.00)), eq("eur"), any());
        }

        @Test
        @DisplayName("uses explicitly provided amount when given")
        void usesProvidedAmountWhenGiven() {
            when(stripeGateway.createPaymentIntent(eq(10L), eq(BigDecimal.valueOf(50.00)), eq("eur"), any()))
                .thenReturn(new StripeGateway.PaymentIntent("pi_456", "cs_456", "requires_payment_method"));

            var response = paymentService.createPaymentIntent(10L, BigDecimal.valueOf(50.00), null);

            assertThat(response.paymentIntentId()).isEqualTo("pi_456");
            verify(bookingRepository, never()).findById(any());
        }

        @Test
        @DisplayName("passes allowed networks to Stripe gateway when specified")
        void passesAllowedNetworksToGateway() {
            when(stripeGateway.createPaymentIntent(eq(10L), eq(BigDecimal.valueOf(85.00)), eq("eur"),
                    eq(List.of("visa", "mastercard"))))
                .thenReturn(new StripeGateway.PaymentIntent("pi_789", "cs_789", "requires_payment_method"));

            var response = paymentService.createPaymentIntent(10L, BigDecimal.valueOf(85.00),
                List.of("visa", "mastercard"));

            assertThat(response.paymentIntentId()).isEqualTo("pi_789");
            verify(stripeGateway).createPaymentIntent(eq(10L), eq(BigDecimal.valueOf(85.00)), eq("eur"),
                eq(List.of("visa", "mastercard")));
        }

        @Test
        @DisplayName("throws when booking not found and no amount provided")
        void throwsWhenBookingNotFoundAndNoAmount() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.createPaymentIntent(999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── confirmPayment ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmPayment()")
    class ConfirmPayment {

        @Test
        @DisplayName("marks payment as paid on succeeded status")
        void marksAsPaid() {
            Payment payment = createPayment(1L, "pi_abc123", Payment.PaymentStatus.PENDING, 100L);
            when(paymentRepository.findByStripePaymentIntentId("pi_abc123"))
                .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            PaymentResponse response = paymentService.confirmPayment("pi_abc123", "succeeded");

            assertThat(response.status())
                .isEqualTo(com.zdravdom.booking.domain.Booking.PaymentStatus.PAID);
        }

        @Test
        @DisplayName("marks payment as failed on non-succeeded status")
        void marksAsFailed() {
            Payment payment = createPayment(1L, "pi_abc123", Payment.PaymentStatus.PENDING, 100L);
            when(paymentRepository.findByStripePaymentIntentId("pi_abc123"))
                .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            PaymentResponse response = paymentService.confirmPayment("pi_abc123", "failed");

            assertThat(response.status())
                .isEqualTo(com.zdravdom.booking.domain.Booking.PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("throws when payment not found")
        void throwsWhenNotFound() {
            when(paymentRepository.findByStripePaymentIntentId("pi_unknown"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment("pi_unknown", "succeeded"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getPaymentHistory ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPaymentHistory()")
    class GetPaymentHistory {

        @Test
        @DisplayName("returns paginated payment list for user")
        void returnsPaymentList() {
            Payment p1 = createPayment(1L, "pi_1", Payment.PaymentStatus.PAID, 1L);
            Payment p2 = createPayment(2L, "pi_2", Payment.PaymentStatus.REFUNDED, 1L);
            when(paymentRepository.findByPatientId(1L)).thenReturn(List.of(p1, p2));

            PaymentListResponse response = paymentService.getPaymentHistory(1L, 0, 10);

            assertThat(response.content()).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no payments")
        void returnsEmptyList() {
            when(paymentRepository.findByPatientId(1L)).thenReturn(List.of());

            PaymentListResponse response = paymentService.getPaymentHistory(1L, 0, 10);

            assertThat(response.content()).isEmpty();
        }
    }

    // ─── processRefund ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("processRefund()")
    class ProcessRefund {

        @Test
        @DisplayName("calls Stripe refund API and marks payment as refunded")
        void processesRefundSuccessfully() {
            Payment payment = createPayment(1L, "pi_abc", Payment.PaymentStatus.PAID, 10L);
            when(paymentRepository.findByBookingId(10L)).thenReturn(List.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
            when(stripeGateway.createRefund(eq("pi_abc"), eq(BigDecimal.valueOf(100))))
                .thenReturn(new StripeGateway.RefundResult("re_123", "succeeded"));

            RefundRequest request = new RefundRequest(10L, BigDecimal.valueOf(100), "Customer request");
            PaymentResponse response = paymentService.processRefund(request);

            assertThat(response.status())
                .isEqualTo(com.zdravdom.booking.domain.Booking.PaymentStatus.REFUNDED);
            verify(stripeGateway).createRefund("pi_abc", BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("throws when no paid payment found for booking")
        void throwsWhenNoPaidPayment() {
            Payment payment = createPayment(1L, "pi_abc", Payment.PaymentStatus.FAILED, 10L);
            when(paymentRepository.findByBookingId(10L)).thenReturn(List.of(payment));

            RefundRequest request = new RefundRequest(10L, BigDecimal.valueOf(100), null);

            assertThatThrownBy(() -> paymentService.processRefund(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No paid payment found");
        }

        @Test
        @DisplayName("throws when no payment found for booking at all")
        void throwsWhenNoPaymentsAtAll() {
            when(paymentRepository.findByBookingId(999L)).thenReturn(List.of());

            RefundRequest request = new RefundRequest(999L, BigDecimal.valueOf(100), null);

            assertThatThrownBy(() -> paymentService.processRefund(request))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("throws when Stripe refund API call fails")
        void throwsWhenStripeRefundsFails() {
            Payment payment = createPayment(1L, "pi_abc", Payment.PaymentStatus.PAID, 10L);
            when(paymentRepository.findByBookingId(10L)).thenReturn(List.of(payment));
            when(stripeGateway.createRefund(eq("pi_abc"), any()))
                .thenThrow(new RuntimeException("Stripe error"));

            RefundRequest request = new RefundRequest(10L, BigDecimal.valueOf(100), null);

            assertThatThrownBy(() -> paymentService.processRefund(request))
                .isInstanceOf(RuntimeException.class);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Payment createPayment(Long id, String stripeId, Payment.PaymentStatus status, Long patientId) {
        Payment p = Payment.create();
        p.setId(id);
        p.setStripePaymentIntentId(stripeId);
        p.setStatus(status);
        p.setAmount(BigDecimal.valueOf(100));
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }
}