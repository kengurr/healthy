package com.zdravdom.user.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.user.application.dto.ProviderResponse;
import com.zdravdom.user.application.dto.ProviderAvailabilityResponse;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest;
import com.zdravdom.user.application.service.ProviderService;
import com.zdravdom.user.application.service.ProviderService.DocumentUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for provider profile management.
 */
@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Provider profiles, availability, and inbox")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current provider profile")
    public ResponseEntity<ProviderResponse> getMyProfile(@AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        ProviderResponse response = providerService.getProviderByUserId(principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/availability")
    @Operation(summary = "Get provider availability")
    public ResponseEntity<ProviderAvailabilityResponse> getMyAvailability(@AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        ProviderAvailabilityResponse response = providerService.getAvailability(principal.userId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/availability")
    @Operation(summary = "Update provider availability")
    public ResponseEntity<ProviderAvailabilityResponse> updateMyAvailability(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        ProviderAvailabilityResponse response = providerService.updateAvailability(principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/documents")
    @Operation(summary = "Upload verification documents")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestParam String type) {
        // In production, this would handle multipart file upload
        DocumentUploadResponse response = providerService.uploadDocument(
            principal.userId(), type, new byte[0], "document.pdf");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get public provider profile")
    public ResponseEntity<ProviderResponse> getProviderProfile(@PathVariable Long id) {
        // For MVP, return mock data. In production, query by provider id
        ProviderResponse response = providerService.getProviderByUserId(1L);
        return ResponseEntity.ok(response);
    }
}