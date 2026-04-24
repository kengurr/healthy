package com.zdravdom.booking.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Booking entity representing a scheduled home healthcare visit.
 * Maps to booking.bookings table.
 *
 * PRODUCTION: Missing soft-delete — booking records require 10-year retention (health data).
 *             Use active flag or deleted_at; anonymize patient/provider data on deletion.
 * PRODUCTION: Missing composite index on (patient_id, date) — used in "upcoming bookings" query.
 * PRODUCTION: Missing composite index on (provider_id, date) — used in provider inbox query.
 * PRODUCTION: Missing index on idempotency_key — already has unique constraint, verify index exists.
 * PRODUCTION: Missing index on status — used for filtering.
 */
@Entity
@DynamicUpdate
@Table(name = "bookings", schema = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate date;

    @Embedded
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    public enum BookingStatus {
        REQUESTED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    }

    public enum PaymentStatus {
        PENDING, PAID, REFUNDED, FAILED
    }

    // Default constructor for JPA
    public Booking() {}

    // Convenience constructor matching old record signature (for mock services)
    public Booking(Long id, Long patientId, Long providerId, Long serviceId,
                   Long packageId, Long addressId, LocalDate date, TimeSlot timeSlot,
                   BookingStatus status, BigDecimal paymentAmount, PaymentStatus paymentStatus,
                   String cancellationReason, LocalDateTime createdAt, LocalDateTime updatedAt,
                   String idempotencyKey) {
        this.id = id;
        this.patientId = patientId;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.packageId = packageId;
        this.addressId = addressId;
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = status;
        this.paymentAmount = paymentAmount;
        this.paymentStatus = paymentStatus;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = BookingStatus.REQUESTED;
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isCancellable() {
        return status == BookingStatus.REQUESTED || status == BookingStatus.CONFIRMED;
    }

    public boolean isEditable() {
        return status == BookingStatus.REQUESTED;
    }

    // Getters
    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Long getProviderId() { return providerId; }
    public Long getServiceId() { return serviceId; }
    public Long getPackageId() { return packageId; }
    public Long getAddressId() { return addressId; }
    public LocalDate getDate() { return date; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getVersion() { return version; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setVersion(Long version) { this.version = version; }
}
