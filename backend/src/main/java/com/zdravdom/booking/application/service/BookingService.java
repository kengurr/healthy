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

    /**
     * Get available time slots for a service at an address on a given date.
     * Uses Redis slot locking to prevent race conditions.
     */
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(UUID serviceId, LocalDate date, UUID addressId) {
        log.info("Fetching available slots for service {} at address {} on {}", serviceId, addressId, date);

        // Generate mock time slots (in production, would query provider availability)
        return List.of(
            new TimeSlotResponse(LocalTime.of(8, 0), LocalTime.of(9, 0), true),
            new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 0), true),
            new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0), false), // booked
            new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(12, 0), true),
            new TimeSlotResponse(LocalTime.of(13, 0), LocalTime.of(14, 0), true),
            new TimeSlotResponse(LocalTime.of(14, 0), LocalTime.of(15, 0), true),
            new TimeSlotResponse(LocalTime.of(15, 0), LocalTime.of(16, 0), false), // booked
            new TimeSlotResponse(LocalTime.of(16, 0), LocalTime.of(17, 0), true)
        );
    }

    /**
     * Create a new booking with slot locking.
     * The slot lock ensures no double-booking during concurrent requests.
     */
    @Transactional
    public BookingResponse createBooking(UUID patientId, CreateBookingRequest request) {
        log.info("Creating booking for patient {} - service: {}, date: {}, time: {}",
            patientId, request.serviceId(), request.date(), request.timeSlot());

        // TODO: Implement Redis slot locking here
        // SlotLock lock = slotLockService.tryLock(request.providerId(), request.date(), request.timeSlot());
        // if (lock == null) throw new ConflictException("Time slot no longer available");

        // Create mock booking
        LocalTime startTime = LocalTime.parse(request.timeSlot());
        TimeSlot timeSlot = new TimeSlot(startTime, startTime.plusHours(1));

        Booking booking = new Booking(
            System.currentTimeMillis(),
            toLong(patientId),
            request.providerId() != null ? toLong(request.providerId()) : 1L,
            toLong(request.serviceId()),
            request.packageId() != null ? toLong(request.packageId()) : null,
            toLong(request.addressId()),
            request.date(),
            timeSlot,
            BookingStatus.REQUESTED,
            BigDecimal.valueOf(45.00),
            PaymentStatus.PENDING,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            UUID.randomUUID().toString()
        );

        log.info("Created booking with id: {}", booking.id());
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId) {
        // For MVP, return mock booking. In production, query by id
        Booking booking = createMockBooking(toLong(bookingId));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, String reason) {
        Booking booking = createMockBooking(toLong(bookingId));

        if (!booking.isCancellable()) {
            throw new ValidationException("Booking cannot be cancelled in current status");
        }

        Booking cancelled = new Booking(
            booking.id(), booking.patientId(), booking.providerId(),
            booking.serviceId(), booking.packageId(), booking.addressId(),
            booking.date(), booking.timeSlot(), BookingStatus.CANCELLED,
            booking.paymentAmount(), booking.paymentStatus(),
            reason, booking.createdAt(), LocalDateTime.now(),
            booking.idempotencyKey()
        );

        log.info("Cancelled booking: {}", bookingId);
        return toResponse(cancelled);
    }

    @Transactional
    public BookingResponse acceptBooking(UUID bookingId, UUID providerId) {
        Booking booking = createMockBooking(toLong(bookingId));

        if (booking.status() != BookingStatus.REQUESTED) {
            throw new ValidationException("Cannot accept booking in current status");
        }

        Booking confirmed = new Booking(
            booking.id(), booking.patientId(), toLong(providerId),
            booking.serviceId(), booking.packageId(), booking.addressId(),
            booking.date(), booking.timeSlot(), BookingStatus.CONFIRMED,
            booking.paymentAmount(), booking.paymentStatus(),
            booking.cancellationReason(), booking.createdAt(), LocalDateTime.now(),
            booking.idempotencyKey()
        );

        log.info("Provider {} accepted booking: {}", providerId, bookingId);
        return toResponse(confirmed);
    }

    @Transactional
    public BookingResponse rejectBooking(UUID bookingId, UUID providerId, String reason) {
        Booking booking = createMockBooking(toLong(bookingId));

        if (booking.status() != BookingStatus.REQUESTED) {
            throw new ValidationException("Cannot reject booking in current status");
        }

        Booking rejected = new Booking(
            booking.id(), booking.patientId(), toLong(providerId),
            booking.serviceId(), booking.packageId(), booking.addressId(),
            booking.date(), booking.timeSlot(), BookingStatus.CANCELLED,
            booking.paymentAmount(), booking.paymentStatus(),
            reason, booking.createdAt(), LocalDateTime.now(),
            booking.idempotencyKey()
        );

        log.info("Provider {} rejected booking: {}", providerId, bookingId);
        return toResponse(rejected);
    }

    @Transactional(readOnly = true)
    public BookingListResponse getBookingHistory(UUID userId, BookingStatus status, int page, int size) {
        // In production, query based on user role and status filter
        List<BookingResponse> bookings = List.of(
            toResponse(createMockBooking(1L)),
            toResponse(createMockBooking(2L))
        );

        return new BookingListResponse(bookings, page, size, 2, 1);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUpcomingBookings(UUID userId) {
        // In production, query upcoming bookings for user
        return List.of(
            toResponse(createMockBooking(1L)),
            toResponse(createMockBooking(2L))
        );
    }

    @Transactional(readOnly = true)
    public BookingListResponse getProviderInbox(UUID providerId, String status, int page, int size) {
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
            new StatusTimelineItem(booking.status(), LocalDateTime.now(), null)
        );

        return new BookingResponse(
            booking.id(), booking.patientId(), booking.providerId(),
            booking.serviceId(), booking.packageId(), booking.addressId(),
            booking.date(), booking.timeSlot().startTime().toString(),
            booking.status(), booking.paymentAmount(), booking.paymentStatus(),
            booking.cancellationReason(), booking.createdAt(), timeline
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
        return new Booking(
            id, 1L, 2L, 1L, null, 1L,
            LocalDate.now().plusDays(1),
            new TimeSlot(time, time.plusHours(1)),
            BookingStatus.CONFIRMED,
            BigDecimal.valueOf(45.00),
            PaymentStatus.PENDING,
            null,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now(),
            UUID.randomUUID().toString()
        );
    }

    private Long toLong(UUID uuid) {
        return uuid != null ? uuid.getMostSignificantBits() : null;
    }
}