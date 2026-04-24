package com.zdravdom.billing.adapters.out.persistence;

import com.zdravdom.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.paidAt >= :start AND p.paidAt < :end")
    BigDecimal sumAmountByStatusBetween(
        @Param("status") Payment.PaymentStatus status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}