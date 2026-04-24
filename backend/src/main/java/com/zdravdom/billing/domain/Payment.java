package com.zdravdom.billing.domain;

import com.zdravdom.booking.domain.Booking;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Provider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment entity for billing. Maps to billing.invoices table.
 * Note: Originally named Payment but DB schema uses 'invoices'.
 *
 * PRODUCTION: Missing soft-delete — billing records require 10-year retention for tax/legal compliance.
 * PRODUCTION: Missing index on patient_id — used in payment history queries.
 * PRODUCTION: Missing index on stripe_payment_intent_id — used in webhook confirmation lookups.
 * PRODUCTION: Missing index on invoice_number — already unique but verify index exists.
 */
@Entity
@Table(name = "invoices", schema = "billing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING, PAID, REFUNDED, FAILED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markAsPaid() {
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    /** Factory method for application-layer instantiation. */
    public static Payment create() {
        return new Payment();
    }
}
