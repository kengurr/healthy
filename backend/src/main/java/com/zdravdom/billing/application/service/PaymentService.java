package com.zdravdom.billing.application.service;

import com.zdravdom.billing.adapters.out.persistence.PaymentRepository;
import com.zdravdom.billing.adapters.out.stripe.StripeGateway;
import com.zdravdom.billing.application.dto.PaymentIntentResponse;
import com.zdravdom.billing.application.dto.PaymentListResponse;
import com.zdravdom.billing.application.dto.PaymentResponse;
import com.zdravdom.billing.application.dto.RefundRequest;
import com.zdravdom.billing.domain.Payment;
import com.zdravdom.billing.domain.Payment.PaymentStatus;
import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.domain.Booking;
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

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final StripeGateway stripeGateway;

    public PaymentService(PaymentRepository paymentRepository,
                         BookingRepository bookingRepository,
                         StripeGateway stripeGateway) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.stripeGateway = stripeGateway;
    }

    @Transactional
    public PaymentIntentResponse createPaymentIntent(Long bookingId, BigDecimal amount,
        List<String> allowedNetworks) {
        log.info("Creating payment intent for booking: {}, amount: {}, networks: {}",
            bookingId, amount, allowedNetworks);

        // Resolve real amount from booking if not explicitly provided
        BigDecimal actualAmount = resolveAmountFromBooking(bookingId, amount);

        StripeGateway.PaymentIntent intent = stripeGateway.createPaymentIntent(
            bookingId, actualAmount, "eur", allowedNetworks); // PRODUCTION: Currency "eur" must come from service/booking config or patient locale, not hardcoded

        return new PaymentIntentResponse(intent.clientSecret(), intent.id());
    }

    @Transactional
    public PaymentResponse confirmPayment(String paymentIntentId, String status) {
        log.info("Confirming payment: {}, status: {}", paymentIntentId, status);

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment with stripePaymentIntentId: " + paymentIntentId));

        PaymentStatus paymentStatus = resolveStatus(status);
        if (paymentStatus == PaymentStatus.PAID) {
            payment.markAsPaid();
        } else if (paymentStatus == PaymentStatus.REFUNDED) {
            payment.markAsRefunded();
        } else {
            payment.markAsFailed();
        }

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentListResponse getPaymentHistory(Long userId, int page, int size) {
        log.info("Fetching payment history for user: {}", userId);

        List<Payment> payments = paymentRepository.findByPatientId(userId);

        List<PaymentResponse> content = payments.stream()
            .map(this::toResponse)
            .toList();

        return new PaymentListResponse(content, page, size, content.size(),
            (int) Math.ceil((double) content.size() / size));
    }

    @Transactional
    public PaymentResponse processRefund(RefundRequest request) {
        log.info("Processing refund for booking: {}, amount: {}",
            request.bookingId(), request.amount());

        List<Payment> payments = paymentRepository.findByBookingId(request.bookingId());
        Payment payment = payments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.PAID)
            .findFirst()
            .orElseThrow(() -> new ValidationException(
                "No paid payment found for booking: " + request.bookingId()));

        // Call Stripe refund API
        StripeGateway.RefundResult refundResult = stripeGateway.createRefund(
            payment.getStripePaymentIntentId(), request.amount());

        // Update local payment status
        payment.markAsRefunded();
        Payment saved = paymentRepository.save(payment);

        log.info("Refund processed for booking: {} (Stripe refund: {})", request.bookingId(), refundResult.id());
        return toResponse(saved);
    }

    // ─── Response mapping ────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getBooking() != null ? payment.getBooking().getId() : null,
            payment.getAmount(),
            "EUR",
            toBookingPaymentStatus(payment.getStatus()),
            payment.getStripePaymentIntentId(),
            payment.getCreatedAt()
        );
    }

    private com.zdravdom.booking.domain.Booking.PaymentStatus toBookingPaymentStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING -> com.zdravdom.booking.domain.Booking.PaymentStatus.PENDING;
            case PAID -> com.zdravdom.booking.domain.Booking.PaymentStatus.PAID;
            case REFUNDED -> com.zdravdom.booking.domain.Booking.PaymentStatus.REFUNDED;
            case FAILED, CANCELLED -> com.zdravdom.booking.domain.Booking.PaymentStatus.FAILED;
        };
    }

    private BigDecimal resolveAmountFromBooking(Long bookingId, BigDecimal requested) {
        if (requested != null && requested.compareTo(BigDecimal.ZERO) > 0) {
            return requested;
        }
        return bookingRepository.findById(bookingId)
            .map(Booking::getPaymentAmount)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private PaymentStatus resolveStatus(String status) {
        return switch (status.toLowerCase()) {
            case "succeeded" -> PaymentStatus.PAID;
            case "refunded" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.FAILED;
        };
    }
}