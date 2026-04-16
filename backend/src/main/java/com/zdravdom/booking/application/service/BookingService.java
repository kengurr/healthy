package com.zdravdom.booking.application.service;

import com.zdravdom.booking.application.dto.*;
import com.zdravdom.booking.application.dto.BookingResponse.StatusTimelineItem;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.Booking.BookingStatus;
import com.zdravdom.booking.domain.Booking.PaymentStatus;
import com.zdravdom.booking.domain.TimeSlot;
import com.zdravdom.global.exception.GlobalExceptionHandler.ConflictException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for booking management including slot locking logic.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(UUID serviceId, LocalDate date, UUID addressId) {
        log.info("Fetching available slots for service {} at address {} on {}", serviceId, addressId, date);
        return List.of(
            new TimeSlotResponse(LocalTime.of(8, 0), LocalTime.of(9, 0), true),
            new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 0), true),
            new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0), false),
            new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(12, 0), true),
            new TimeSlotResponse(LocalTime.of(13, 0), LocalTime.of(14, 0), true),
            new TimeSlotResponse(LocalTime.of(14, 0), LocalTime.of(15, 0), true),
            new TimeSlotResponse(LocalTime.of(15, 0), LocalTime.of(16, 0), false),
            new TimeSlotResponse(LocalTime.of(16, 0), LocalTime.of(17, 0), true)
        );
    }

    @Transactional
    public BookingResponse createBooking(Long patientId, CreateBookingRequest request) {
        log.info("Creating booking for patient {} - service: {}, date: {}, time: {}",
            patientId, request.serviceId(), request.date(), request.timeSlot());

        LocalTime startTime = LocalTime.parse(request.timeSlot());
        TimeSlot timeSlot = new TimeSlot(startTime, startTime.plusHours(1));

        Booking booking = new Booking();
        booking.setId(System.currentTimeMillis());
        booking.setPatientId(patientId);
        booking.setProviderId(request.providerId() != null ? request.providerId() : 1L);
        booking.setServiceId(request.serviceId());
        booking.setPackageId(request.packageId());
        booking.setAddressId(request.addressId());
        booking.setDate(request.date());
        booking.setTimeSlot(timeSlot);
        booking.setStatus(BookingStatus.REQUESTED);
        booking.setPaymentAmount(BigDecimal.valueOf(45.00));
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setIdempotencyKey(UUID.randomUUID().toString());

        log.info("Created booking with id: {}", booking.getId());
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = createMockBooking(bookingId);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = createMockBooking(bookingId);

        if (!booking.isCancellable()) {
            throw new ValidationException("Booking cannot be cancelled in current status");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());

        log.info("Cancelled booking: {}", bookingId);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse acceptBooking(Long bookingId, Long providerId) {
        Booking booking = createMockBooking(bookingId);

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new ValidationException("Cannot accept booking in current status");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setProviderId(providerId);
        booking.setUpdatedAt(LocalDateTime.now());

        log.info("Provider {} accepted booking: {}", providerId, bookingId);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse rejectBooking(Long bookingId, Long providerId, String reason) {
        Booking booking = createMockBooking(bookingId);

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new ValidationException("Cannot reject booking in current status");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setProviderId(providerId);
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());

        log.info("Provider {} rejected booking: {}", providerId, bookingId);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingListResponse getBookingHistory(Long userId, BookingStatus status, int page, int size) {
        List<BookingResponse> bookings = List.of(
            toResponse(createMockBooking(1L)),
            toResponse(createMockBooking(2L))
        );
        return new BookingListResponse(bookings, page, size, 2, 1);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUpcomingBookings(Long userId) {
        return List.of(
            toResponse(createMockBooking(1L)),
            toResponse(createMockBooking(2L))
        );
    }

    @Transactional(readOnly = true)
    public BookingListResponse getProviderInbox(Long providerId, String status, int page, int size) {
        List<BookingInboxItem> items = List.of(
            new BookingInboxItem(
                1L, 1L, "Janez", "Novak", "Home Nursing Care",
                new BookingInboxItem.AddressSummary("Ljubljanska 10", "Ljubljana", "1000"),
                LocalDate.now().plusDays(1), "09:00",
                BookingStatus.REQUESTED
            ),
            new BookingInboxItem(
                2L, 2L, "Ana", "Kovač", "Physiotherapy Session",
                new BookingInboxItem.AddressSummary("Celjska 5", "Maribor", "2000"),
                LocalDate.now().plusDays(2), "14:00",
                BookingStatus.CONFIRMED
            )
        );
        return new BookingListResponse(
            items.stream().map(this::toResponse).toList(),
            page, size, items.size(), 1
        );
    }

    private BookingResponse toResponse(Booking booking) {
        List<StatusTimelineItem> timeline = List.of(
            new StatusTimelineItem(booking.getStatus(), LocalDateTime.now(), null)
        );
        return new BookingResponse(
            booking.getId(),
            booking.getPatientId(),
            booking.getProviderId(),
            booking.getServiceId(),
            booking.getPackageId(),
            booking.getAddressId(),
            booking.getDate(),
            booking.getTimeSlot() != null ? booking.getTimeSlot().getStartTime().toString() : null,
            booking.getStatus(),
            booking.getPaymentAmount(),
            booking.getPaymentStatus(),
            booking.getCancellationReason(),
            booking.getCreatedAt(),
            timeline
        );
    }

    private BookingResponse toResponse(BookingInboxItem item) {
        return new BookingResponse(
            item.id(), item.patientId(), null,
            null, null, null,
            item.date(), item.timeSlot(),
            item.status(), null, null,
            null, LocalDateTime.now(), List.of()
        );
    }

    private Booking createMockBooking(Long id) {
        LocalTime time = LocalTime.of(9, 0);
        Booking booking = new Booking();
        booking.setId(id);
        booking.setPatientId(1L);
        booking.setProviderId(2L);
        booking.setServiceId(1L);
        booking.setPackageId(null);
        booking.setAddressId(1L);
        booking.setDate(LocalDate.now().plusDays(1));
        booking.setTimeSlot(new TimeSlot(time, time.plusHours(1)));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentAmount(BigDecimal.valueOf(45.00));
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now().minusHours(2));
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setIdempotencyKey(UUID.randomUUID().toString());
        return booking;
    }
}
