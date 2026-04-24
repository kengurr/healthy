package com.zdravdom.booking.adapters.inbound.rest;

import com.zdravdom.booking.application.dto.AdminBookingResponse;
import com.zdravdom.booking.application.service.AdminBookingService;
import com.zdravdom.booking.domain.Booking;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for booking management — admin-wide booking list, assign provider, cancel.
 */
@RestController
@RequestMapping("/api/v1/admin/bookings")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    @Operation(summary = "List all bookings", description = "Returns all bookings, optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking list"),
        @ApiResponse(responseCode = "403", description = "Forbidden — OPERATOR, ADMIN, SUPERADMIN only")
    })
    public ResponseEntity<List<Booking>> listBookings(
            @RequestParam(required = false) Booking.BookingStatus status) {
        return ResponseEntity.ok(adminBookingService.getAllBookings(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking detail with timeline"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<AdminBookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(adminBookingService.getBookingById(id));
    }

    @PutMapping("/{id}/assign-provider")
    @Operation(summary = "Assign or reassign a provider to a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Provider assigned"),
        @ApiResponse(responseCode = "404", description = "Booking or provider not found"),
        @ApiResponse(responseCode = "400", description = "Provider is not active")
    })
    public ResponseEntity<AdminBookingResponse> assignProvider(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long providerId = ((Number) body.get("providerId")).longValue();
        String reason = (String) body.get("reason");
        return ResponseEntity.ok(adminBookingService.assignProvider(id, providerId, reason));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Booking cancelled"),
        @ApiResponse(responseCode = "400", description = "Booking cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Cancelled by admin";
        adminBookingService.cancelBooking(id, reason);
        return ResponseEntity.noContent().build();
    }
}