package com.zdravdom.user.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.booking.application.dto.BookingListResponse;
import com.zdravdom.booking.application.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for provider booking inbox.
 */
@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Provider booking inbox")
public class ProviderInboxController {

    private final BookingService bookingService;

    public ProviderInboxController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/inbox")
    @Operation(summary = "Provider's booking inbox")
    public ResponseEntity<BookingListResponse> getInbox(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        BookingListResponse response = bookingService.getProviderInbox(
            principal.userId(), status, page, size);
        return ResponseEntity.ok(response);
    }
}