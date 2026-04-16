package com.zdravdom.booking.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Booking entity representing a scheduled home healthcare visit.
 */
public record Booking(
    Long id,
    Long patientId,
    Long providerId,
    Long serviceId,
    Long packageId,
    Long addressId,
    LocalDate date,
    TimeSlot timeSlot,
    BookingStatus status,
    BigDecimal paymentAmount,
    PaymentStatus paymentStatus,
    String cancellationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String idempotencyKey
) {
    public enum BookingStatus {
        REQUESTED,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        REFUNDED,
        FAILED
    }

    public Booking {
        if (status == null) status = BookingStatus.REQUESTED;
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
        if (timeSlot == null) {
            timeSlot = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0));
        }
    }

    public boolean isCancellable() {
        return status == BookingStatus.REQUESTED || status == BookingStatus.CONFIRMED;
    }

    public boolean isEditable() {
        return status == BookingStatus.REQUESTED;
    }

    public Booking withStatus(BookingStatus newStatus) {
        return new Booking(
            id, patientId, providerId, serviceId, packageId, addressId,
            date, timeSlot, newStatus, paymentAmount, paymentStatus,
            cancellationReason, createdAt, LocalDateTime.now(), idempotencyKey
        );
    }
}
