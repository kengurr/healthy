package com.zdravdom.booking.application.dto;

import com.zdravdom.booking.domain.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking detail response for admin view.
 */
public record AdminBookingResponse(
    Long id,
    Long patientId,
    Long providerId,
    Long serviceId,
    Long addressId,
    LocalDate date,
    String timeSlot,
    Booking.BookingStatus status,
    Booking.PaymentStatus paymentStatus,
    BigDecimal paymentAmount,
    String cancellationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<StatusTimelineItem> timeline
) {
    public record StatusTimelineItem(
        Booking.BookingStatus status,
        LocalDateTime timestamp,
        String note
    ) {}
}