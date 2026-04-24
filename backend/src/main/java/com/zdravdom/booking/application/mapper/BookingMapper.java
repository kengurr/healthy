package com.zdravdom.booking.application.mapper;

import com.zdravdom.booking.application.dto.CreateBookingRequest;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.Booking.BookingStatus;
import com.zdravdom.booking.domain.Booking.PaymentStatus;
import com.zdravdom.booking.domain.StatusTimelineEntry;
import com.zdravdom.booking.domain.TimeSlot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Maps CreateBookingRequest to Booking entity and manages status timeline entries.
 * Stateless — safe to use directly in transactional service methods.
 */
public final class BookingMapper {

    private BookingMapper() {}

    /**
     * Create a new Booking from a CreateBookingRequest.
     * Sets initial status to REQUESTED, payment to PENDING.
     * Does NOT save — caller is responsible for persistence.
     */
    public static Booking createFromRequest(Long patientId, CreateBookingRequest request,
            String idempotencyKey, BigDecimal paymentAmount) {
        LocalTime startTime = LocalTime.parse(request.timeSlot());
        TimeSlot timeSlot = new TimeSlot(startTime, startTime.plusHours(1));

        Booking booking = new Booking();
        booking.setPatientId(patientId);
        booking.setProviderId(request.providerId() != null ? request.providerId() : null);
        booking.setServiceId(request.serviceId());
        booking.setPackageId(request.packageId());
        booking.setAddressId(request.addressId());
        booking.setDate(request.date());
        booking.setTimeSlot(timeSlot);
        booking.setStatus(BookingStatus.REQUESTED);
        booking.setPaymentAmount(paymentAmount);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }

    /**
     * Create a status timeline entry for a booking state transition.
     */
    public static StatusTimelineEntry createTimelineEntry(Long bookingId, BookingStatus status,
            String note, Long createdBy) {
        return new StatusTimelineEntry(bookingId, status, note, createdBy);
    }

    /**
     * Apply cancellation to a booking — sets status and reason.
     * Status must already be validated as cancellable by the caller.
     */
    public static void applyCancellation(Booking booking, String reason) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Apply provider acceptance to a booking.
     * Status must already be validated as REQUESTED by the caller.
     */
    public static void applyAcceptance(Booking booking, Long providerId) {
        booking.setProviderId(providerId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Apply provider rejection to a booking.
     * Status must already be validated as REQUESTED by the caller.
     */
    public static void applyRejection(Booking booking, Long providerId, String reason) {
        booking.setProviderId(providerId);
        booking.setCancellationReason(reason);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
    }
}
