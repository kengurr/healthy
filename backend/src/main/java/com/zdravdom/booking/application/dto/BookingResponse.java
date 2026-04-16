package com.zdravdom.booking.application.dto;

import com.zdravdom.booking.domain.Booking.BookingStatus;
import com.zdravdom.booking.domain.Booking.PaymentStatus;
import com.zdravdom.booking.domain.TimeSlot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking response DTO.
 */
public record BookingResponse(
    Long id,
    Long patientId,
    Long providerId,
    Long serviceId,
    Long packageId,
    Long addressId,
    LocalDate date,
    String timeSlot,
    BookingStatus status,
    BigDecimal paymentAmount,
    PaymentStatus paymentStatus,
    String cancellationReason,
    LocalDateTime createdAt,
    List<StatusTimelineItem> statusTimeline
) {
    public record StatusTimelineItem(
        BookingStatus status,
        LocalDateTime timestamp,
        String note
    ) {}
}