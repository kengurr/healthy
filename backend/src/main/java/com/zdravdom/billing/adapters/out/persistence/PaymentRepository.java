package com.zdravdom.billing.adapters.out.persistence;

import com.zdravdom.billing.domain.Payment;
import com.zdravdom.booking.domain.Booking.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Payment entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPatientId(Long patientId);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Payment> findByStatus(PaymentStatus status);
}