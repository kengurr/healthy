package com.zdravdom.user.adapters.inbound.rest;

import com.zdravdom.user.application.dto.ProviderVerificationItem;
import com.zdravdom.user.application.service.AdminProviderService;
import com.zdravdom.user.domain.Provider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for provider management — verification queue, approve/reject.
 */
@RestController
@RequestMapping("/api/v1/admin/providers")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(AdminProviderService adminProviderService) {
        this.adminProviderService = adminProviderService;
    }

    @GetMapping
    @Operation(summary = "List all providers", description = "Returns all providers, optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Provider list"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Provider>> listProviders(
            @RequestParam(required = false) Provider.ProviderStatus status) {
        return ResponseEntity.ok(adminProviderService.getAllProviders(status));
    }

    @GetMapping("/verification-queue")
    @Operation(summary = "Providers pending verification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification queue"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ProviderVerificationItem>> getVerificationQueue() {
        return ResponseEntity.ok(adminProviderService.getVerificationQueue());
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve and activate a provider")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Provider approved"),
        @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<Void> approveProvider(@PathVariable Long id) {
        adminProviderService.approveProvider(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a provider")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Provider rejected"),
        @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<Void> rejectProvider(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        adminProviderService.rejectProvider(id, reason);
        return ResponseEntity.ok().build();
    }
}