package com.zdravdom.booking.application.service;

import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.adapters.out.persistence.StatusTimelineRepository;
import com.zdravdom.booking.application.dto.*;
import com.zdravdom.booking.application.dto.BookingResponse.StatusTimelineItem;
import com.zdravdom.booking.application.mapper.BookingMapper;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.Booking.BookingStatus;
import com.zdravdom.booking.domain.StatusTimelineEntry;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.global.exception.GlobalExceptionHandler.ConflictException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.matching.application.service.SlotLockService;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for booking management including slot locking logic.
 *
 * <p>Status transitions:
 * <ul>
 *   <li>REQUESTED → CONFIRMED (provider accepts)</li>
 *   <li>REQUESTED → CANCELLED (patient cancels, provider rejects)</li>
 *   <li>CONFIRMED → IN_PROGRESS (provider starts visit)</li>
 *   <li>IN_PROGRESS → COMPLETED (provider submits visit report)</li>
 * </ul>
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final StatusTimelineRepository timelineRepository;
    private final ServiceRepository serviceRepository;
    private final PatientRepository patientRepository;
    private final SlotLockService slotLockService;

    public BookingService(
            BookingRepository bookingRepository,
            StatusTimelineRepository timelineRepository,
            ServiceRepository serviceRepository,
            PatientRepository patientRepository,
            SlotLockService slotLockService) {
        this.bookingRepository = bookingRepository;
        this.timelineRepository = timelineRepository;
        this.serviceRepository = serviceRepository;
        this.patientRepository = patientRepository;
        this.slotLockService = slotLockService;
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(UUID serviceId, LocalDate date, UUID addressId) {
        log.info("Fetching available slots for service {} at address {} on {}", serviceId, addressId, date);

        List<Booking> existing = bookingRepository.findByDate(date);
        return computeAvailableSlots(date, existing);
    }

    @Transactional
    public BookingResponse createBooking(Long patientId, CreateBookingRequest request) {
        log.info("Creating booking for patient {} - service: {}, date: {}, time: {}",
            patientId, request.serviceId(), request.date(), request.timeSlot());

        // Validate patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }

        // Validate service exists
        if (!serviceRepository.existsById(request.serviceId())) {
            throw new ResourceNotFoundException("Service", request.serviceId());
        }

        // Deterministic idempotency key: patientId + serviceId + date + timeSlot
        String idempotencyKey = buildIdempotencyKey(patientId, request);

        // Check for duplicate using idempotency key
        if (bookingRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new ConflictException("Booking already exists for this request");
        }

        // Check for time slot conflict before booking (race condition guard)
        LocalTime requestedTime = parseTime(request.timeSlot());
        LocalTime endTime = requestedTime.plusHours(1);
        List<Booking> existingOnDate = bookingRepository.findByDate(request.date());
        boolean hasConflict = existingOnDate.stream()
            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
            .anyMatch(b -> {
                if (b.getTimeSlot() == null) return false;
                var slot = new com.zdravdom.booking.domain.TimeSlot(requestedTime, endTime);
                return b.getTimeSlot().overlaps(slot);
            });
        if (hasConflict) {
            throw new ConflictException("Time slot already booked for this date");
        }

        // Slot locking is per-provider — at booking creation the provider is not yet assigned.
        // Slot locking will occur in MatchingService when a provider accepts the booking.
        // For now, we rely on the idempotency key + conflict check above.

        // Fetch price from service
        BigDecimal paymentAmount = serviceRepository.findById(request.serviceId())
            .map(s -> s.getPrice())
            .orElse(BigDecimal.ZERO);

        Booking booking = BookingMapper.createFromRequest(
            patientId, request, idempotencyKey, paymentAmount);

        Booking saved = bookingRepository.save(booking);

        StatusTimelineEntry timeline = BookingMapper.createTimelineEntry(
            saved.getId(), BookingStatus.REQUESTED, "Booking created", patientId);
        timelineRepository.save(timeline);

        log.info("Created booking with id: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.isCancellable()) {
            throw new ValidationException("Booking cannot be cancelled in current status: " + booking.getStatus());
        }

        if (!booking.getPatientId().equals(userId)) {
            throw new ValidationException("Not authorized to cancel this booking");
        }

        BookingMapper.applyCancellation(booking, reason);
        Booking saved = bookingRepository.save(booking);

        StatusTimelineEntry timeline = BookingMapper.createTimelineEntry(
            saved.getId(), BookingStatus.CANCELLED,
            reason != null ? "Cancelled: " + reason : "Cancelled by patient", userId);
        timelineRepository.save(timeline);

        log.info("Cancelled booking: {}", bookingId);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse acceptBooking(Long bookingId, Long providerId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new ValidationException("Cannot accept booking in current status: " + booking.getStatus());
        }

        if (booking.getTimeSlot() == null) {
            throw new ValidationException("Booking has no time slot assigned");
        }

        LocalTime timeSlot = booking.getTimeSlot().getStartTime();

        // Acquire Redis slot lock before confirming — prevents double-booking of this provider's time slot
        boolean locked = slotLockService.tryLock(
            providerId,
            booking.getDate(),
            timeSlot,
            "accept:" + booking.getIdempotencyKey()
        );
        if (!locked) {
            throw new ConflictException("Time slot is no longer available — another booking was confirmed");
        }

        try {
            BookingMapper.applyAcceptance(booking, providerId);
            Booking saved = bookingRepository.save(booking);

            StatusTimelineEntry timeline = BookingMapper.createTimelineEntry(
                saved.getId(), BookingStatus.CONFIRMED, "Accepted by provider", providerId);
            timelineRepository.save(timeline);

            log.info("Provider {} accepted booking: {}", providerId, bookingId);
            return toResponse(saved);
        } catch (RuntimeException e) {
            // Release the slot lock if booking confirmation fails
            slotLockService.releaseLock(providerId, booking.getDate(), timeSlot);
            throw e;
        }
    }

    @Transactional
    public BookingResponse rejectBooking(Long bookingId, Long providerId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new ValidationException("Cannot reject booking in current status: " + booking.getStatus());
        }

        BookingMapper.applyRejection(booking, providerId, reason);
        Booking saved = bookingRepository.save(booking);

        StatusTimelineEntry timeline = BookingMapper.createTimelineEntry(
            saved.getId(), BookingStatus.CANCELLED,
            reason != null ? "Rejected: " + reason : "Rejected by provider", providerId);
        timelineRepository.save(timeline);

        log.info("Provider {} rejected booking: {}", providerId, bookingId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BookingListResponse getBookingHistory(Long userId, BookingStatus status, int page, int size) {
        List<Booking> bookings;

        if (status != null) {
            bookings = bookingRepository.findByPatientIdAndStatus(userId, status);
        } else {
            bookings = bookingRepository.findByPatientId(userId);
        }

        List<BookingResponse> content = bookings.stream()
            .map(this::toResponse)
            .toList();

        return new BookingListResponse(content, page, size, content.size(),
            (int) Math.ceil((double) content.size() / size));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUpcomingBookings(Long userId) {
        LocalDate today = LocalDate.now();
        return bookingRepository.findByPatientId(userId).stream()
            .filter(b -> b.getDate().isAfter(today) || b.getDate().isEqual(today))
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                      || b.getStatus() == BookingStatus.REQUESTED)
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public BookingListResponse getProviderInbox(Long providerId, String status, int page, int size) {
        List<Booking> bookings;

        if (status != null && !status.equalsIgnoreCase("ALL")) {
            BookingStatus bs = BookingStatus.valueOf(status.toUpperCase());
            bookings = bookingRepository.findByProviderId(providerId).stream()
                .filter(b -> b.getStatus() == bs)
                .toList();
        } else {
            bookings = bookingRepository.findByProviderId(providerId);
        }

        List<BookingResponse> content = bookings.stream()
            .map(this::toResponse)
            .toList();

        return new BookingListResponse(content, page, size, content.size(),
            (int) Math.ceil((double) content.size() / size));
    }

    // ─── Response mapping ────────────────────────────────────────────────────

    private BookingResponse toResponse(Booking booking) {
        List<StatusTimelineEntry> timelineEntries =
            timelineRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());

        List<StatusTimelineItem> timeline = timelineEntries.stream()
            .map(e -> new StatusTimelineItem(e.getStatus(), e.getCreatedAt(), e.getNote()))
            .toList();

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

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Deterministic idempotency key from booking content.
     * Same inputs always produce the same key — enabling true idempotent retries.
     */
    private String buildIdempotencyKey(Long patientId, CreateBookingRequest request) {
        return String.format("booking:%d:%s:%s:%s",
            patientId,
            request.serviceId(),
            request.date(),
            request.timeSlot());
    }

    /**
     * Fixed 1-hour slots 08:00–17:00 excluding 12:00 lunch.
     * Marks slots as unavailable when an existing confirmed booking overlaps.
     */
    private List<TimeSlotResponse> computeAvailableSlots(LocalDate date, List<Booking> existingBookings) {
        LocalTime[] fixedSlots = {
            LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
            LocalTime.of(11, 0), LocalTime.of(13, 0), LocalTime.of(14, 0),
            LocalTime.of(15, 0), LocalTime.of(16, 0)
        }; // PRODUCTION: Business hours must be configurable per provider or global service config

        return java.util.Arrays.stream(fixedSlots)
            .map(start -> {
                LocalTime end = start.plusHours(1);
                boolean booked = existingBookings.stream()
                    .filter(b -> b.getDate().equals(date))
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                    .anyMatch(b -> b.getTimeSlot() != null
                        && b.getTimeSlot().overlaps(
                            new com.zdravdom.booking.domain.TimeSlot(start, end)));
                return new TimeSlotResponse(start, end, !booked);
            })
            .toList();
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            return LocalTime.of(8, 0); // PRODUCTION: Silent fallback masks invalid input — throw ValidationException instead
        }
    }
}
