package com.zdravdom.visit.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.visit.application.dto.*;
import com.zdravdom.visit.application.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for visit management.
 */
@RestController
@RequestMapping("/api/v1/visits")
@Tag(name = "Visits", description = "Visit lifecycle, clinical forms, escalation")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get visit details")
    public ResponseEntity<VisitResponse> getVisit(@PathVariable Long id) {
        VisitResponse response = visitService.getVisitById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "Provider starts visit (GPS location captured)")
    public ResponseEntity<VisitResponse> startVisit(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        VisitResponse response = visitService.startVisit(id, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Submit visit form with vitals, notes, and signature")
    public ResponseEntity<VisitResponse> completeVisit(
            @PathVariable Long id,
            @Valid @RequestBody CompleteVisitRequest request) {
        VisitResponse response = visitService.completeVisit(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Trigger an escalation for a visit")
    public ResponseEntity<EscalationResponse> escalateVisit(
            @PathVariable Long id,
            @Valid @RequestBody EscalationRequest request) {
        EscalationResponse response = visitService.escalateVisit(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download visit report PDF")
    public ResponseEntity<Resource> getVisitPdf(@PathVariable Long id) {
        Resource pdf = visitService.getVisitPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=visit-report-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/send-to-patient")
    @Operation(summary = "Send visit report to patient via email")
    public ResponseEntity<MessageResponse> sendReportToPatient(@PathVariable Long id) {
        visitService.sendReportToPatient(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new MessageResponse("Report email queued"));
    }

    @PostMapping("/{id}/rating")
    @Operation(summary = "Rate a completed visit")
    public ResponseEntity<VisitService.RatingResponse> rateVisit(
            @PathVariable Long id,
            @Valid @RequestBody CreateRatingRequest request) {
        VisitService.RatingResponse response = visitService.rateVisit(id, request.rating(), request.review());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public record MessageResponse(String message) {}
}