package com.zdravdom.booking.application.service;

import com.zdravdom.booking.adapters.out.persistence.BookingRepository;
import com.zdravdom.booking.adapters.out.persistence.StatusTimelineRepository;
import com.zdravdom.booking.application.dto.AdminBookingResponse;
import com.zdravdom.booking.application.dto.AdminBookingResponse.StatusTimelineItem;
import com.zdravdom.booking.domain.Booking;
import com.zdravdom.booking.domain.StatusTimelineEntry;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.domain.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin service for booking management — admin-wide booking list, provider assignment.
 */
@Service
public class AdminBookingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingService.class);

    private final BookingRepository bookingRepository;
    private final StatusTimelineRepository timelineRepository;
    private final ProviderRepository providerRepository;

    public AdminBookingService(
            BookingRepository bookingRepository,
            StatusTimelineRepository timelineRepository,
            ProviderRepository providerRepository) {
        this.bookingRepository = bookingRepository;
        this.timelineRepository = timelineRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings(Booking.BookingStatus status) {
        if (status != null) {
            return bookingRepository.findByStatus(status);
        }
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AdminBookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        List<StatusTimelineEntry> timeline = timelineRepository.findByBookingIdOrderByCreatedAtAsc(id);
        List<StatusTimelineItem> timelineItems = timeline.stream()
            .map(e -> new StatusTimelineItem(e.getStatus(), e.getCreatedAt(), e.getNote()))
            .collect(Collectors.toList());
        return toAdminResponse(booking, timelineItems);
    }

    @Transactional
    public AdminBookingResponse assignProvider(Long bookingId, Long providerId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));

        if (provider.getStatus() != Provider.ProviderStatus.ACTIVE) {
            throw new ValidationException("Cannot assign to inactive provider");
        }

        booking.setProviderId(providerId);
        bookingRepository.save(booking);

        // Audit note
        if (reason != null) {
            timelineRepository.save(new StatusTimelineEntry(bookingId, Booking.BookingStatus.CONFIRMED, reason));
        }

        log.info("Admin assigned provider {} to booking {}", providerId, bookingId);
        return getBookingById(bookingId);
    }

    @Transactional
    public void cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.isCancellable()) {
            throw new ValidationException("Booking cannot be cancelled in status: " + booking.getStatus());
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        bookingRepository.save(booking);
        timelineRepository.save(new StatusTimelineEntry(bookingId, Booking.BookingStatus.CANCELLED, reason));
        log.info("Admin cancelled booking {}: {}", bookingId, reason);
    }

    private AdminBookingResponse toAdminResponse(Booking booking, List<StatusTimelineItem> timeline) {
        return new AdminBookingResponse(
            booking.getId(),
            booking.getPatientId(),
            booking.getProviderId(),
            booking.getServiceId(),
            booking.getAddressId(),
            booking.getDate(),
            booking.getTimeSlot() != null ? booking.getTimeSlot().toString() : null,
            booking.getStatus(),
            booking.getPaymentStatus(),
            booking.getPaymentAmount(),
            booking.getCancellationReason(),
            booking.getCreatedAt(),
            booking.getUpdatedAt(),
            timeline
        );
    }
}