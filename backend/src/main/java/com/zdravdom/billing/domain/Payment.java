package com.zdravdom.billing.domain;

import com.zdravdom.booking.domain.Booking;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Provider;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment entity for billing. Maps to billing.invoices table.
 * Note: Originally named Payment but DB schema uses 'invoices'.
 */
@Entity
@Table(name = "invoices", schema = "billing")
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

    // Default constructor for JPA
    public Payment() {}

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

    // Getters
    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public Patient getPatient() { return patient; }
    public Provider getProvider() { return provider; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public PaymentStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
