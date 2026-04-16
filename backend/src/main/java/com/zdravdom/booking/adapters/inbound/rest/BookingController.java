package com.zdravdom.booking.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.booking.application.dto.*;
import com.zdravdom.booking.application.service.BookingService;
import com.zdravdom.booking.domain.Booking.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for booking management.
 */
@RestController
@RequestMapping("/api/v1/booking")
@Tag(name = "Booking", description = "Booking CRUD, slots, accept/reject")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/slots")
    @Operation(summary = "Get available time slots for a service at an address")
    public ResponseEntity<List<TimeSlotResponse>> getSlots(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam UUID addressId) {
        List<TimeSlotResponse> slots = bookingService.getAvailableSlots(serviceId, date, addressId);
        return ResponseEntity.ok(slots);
    }

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestBody(required = false) CancelBookingRequest request) {
        String reason = request != null ? request.reason() : null;
        BookingResponse response = bookingService.cancelBooking(id, principal.userId(), reason);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "Provider accepts a booking")
    public ResponseEntity<BookingResponse> acceptBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        BookingResponse response = bookingService.acceptBooking(id, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Provider rejects a booking")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestBody RejectBookingRequest request) {
        BookingResponse response = bookingService.rejectBooking(id, principal.userId(), request.reason());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Paginated booking history")
    public ResponseEntity<BookingListResponse> getHistory(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        BookingListResponse response = bookingService.getBookingHistory(principal.userId(), status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get active/upcoming bookings")
    public ResponseEntity<List<BookingResponse>> getUpcoming(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        List<BookingResponse> bookings = bookingService.getUpcomingBookings(principal.userId());
        return ResponseEntity.ok(bookings);
    }

    public record CancelBookingRequest(String reason) {}
    public record RejectBookingRequest(String reason) {}
}