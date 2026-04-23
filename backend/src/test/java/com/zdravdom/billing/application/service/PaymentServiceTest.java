package com.zdravdom.billing.application.service;

import com.zdravdom.billing.adapters.out.persistence.PaymentRepository;
import com.zdravdom.billing.application.dto.PaymentIntentResponse;
import com.zdravdom.billing.application.dto.PaymentListResponse;
import com.zdravdom.billing.application.dto.PaymentResponse;
import com.zdravdom.billing.application.dto.RefundRequest;
import com.zdravdom.billing.domain.Payment;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository);
    }

    // ─── createPaymentIntent ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createPaymentIntent()")
    class CreatePaymentIntent {

        @Test
        @DisplayName("returns mock payment intent response")
        void returnsPaymentIntent() {
            PaymentIntentResponse response = paymentService.createPaymentIntent(1L, BigDecimal.valueOf(150));

            assertThat(response.clientSecret()).startsWith("pi_mock_");
            assertThat(response.paymentIntentId()).startsWith("pi_mock_");
        }
    }

    // ─── confirmPayment ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmPayment()")
    class ConfirmPayment {

        @Test
        @DisplayName("marks payment as paid on success")
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
        @DisplayName("marks payment as failed on error status")
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
        @DisplayName("processes refund for paid payment")
        void processesRefund() {
            Payment payment = createPayment(1L, "pi_abc", Payment.PaymentStatus.PAID, 10L);
            when(paymentRepository.findByBookingId(10L)).thenReturn(List.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            RefundRequest request = new RefundRequest(10L, BigDecimal.valueOf(100), "Customer request");
            PaymentResponse response = paymentService.processRefund(request);

            assertThat(response.status())
                .isEqualTo(com.zdravdom.booking.domain.Booking.PaymentStatus.REFUNDED);
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
