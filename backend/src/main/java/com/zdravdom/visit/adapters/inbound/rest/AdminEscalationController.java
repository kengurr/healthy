package com.zdravdom.visit.adapters.inbound.rest;

import com.zdravdom.visit.application.dto.AdminEscalationResponse;
import com.zdravdom.visit.application.dto.UpdateEscalationStatusRequest;
import com.zdravdom.visit.application.service.AdminEscalationService;
import com.zdravdom.visit.domain.Escalation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for escalation queue management.
 */
@RestController
@RequestMapping("/api/v1/admin/escalations")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminEscalationController {

    private final AdminEscalationService adminEscalationService;

    public AdminEscalationController(AdminEscalationService adminEscalationService) {
        this.adminEscalationService = adminEscalationService;
    }

    @GetMapping
    @Operation(summary = "List all escalations", description = "Returns all escalations, optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Escalation list"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Escalation>> listEscalations(
            @RequestParam(required = false) Escalation.EscalationStatus status) {
        return ResponseEntity.ok(adminEscalationService.getAllEscalations(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get escalation details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Escalation detail"),
        @ApiResponse(responseCode = "404", description = "Escalation not found")
    })
    public ResponseEntity<AdminEscalationResponse> getEscalation(@PathVariable Long id) {
        return ResponseEntity.ok(adminEscalationService.getEscalationById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update escalation status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Escalation updated"),
        @ApiResponse(responseCode = "404", description = "Escalation not found")
    })
    public ResponseEntity<AdminEscalationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateEscalationStatusRequest request) {
        return ResponseEntity.ok(adminEscalationService.updateStatus(id, request));
    }
}