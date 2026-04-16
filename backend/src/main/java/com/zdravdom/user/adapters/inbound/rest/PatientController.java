package com.zdravdom.user.adapters.inbound.rest;

import com.zdravdom.auth.adapters.inbound.security.JwtAuthenticationFilter.JwtAuthenticatedPrincipal;
import com.zdravdom.user.application.dto.PatientResponse;
import com.zdravdom.user.application.dto.UpdatePatientRequest;
import com.zdravdom.user.application.dto.AddressRequest;
import com.zdravdom.user.application.dto.PatientResponse.AddressResponse;
import com.zdravdom.user.application.service.PatientService;
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
 * REST controller for patient profile management.
 */
@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Patient profile and address management")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current patient profile")
    public ResponseEntity<PatientResponse> getMyProfile(@AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        PatientResponse response = patientService.getPatientByUserId(principal.userId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current patient profile")
    public ResponseEntity<PatientResponse> updateMyProfile(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @Valid @RequestBody UpdatePatientRequest request) {
        PatientResponse response = patientService.updatePatient(principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/addresses")
    @Operation(summary = "List saved addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(@AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        List<AddressResponse> addresses = patientService.getAddresses(principal.userId());
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = patientService.addAddress(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/me/documents")
    @Operation(summary = "Upload a medical document")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @AuthenticationPrincipal JwtAuthenticatedPrincipal principal,
            @RequestParam String type,
            @RequestParam String description) {
        // In production, this would handle multipart file upload
        var response = new DocumentUploadResponse(UUID.randomUUID(), "https://s3.example.com/doc/" + UUID.randomUUID());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/gdpr/export")
    @Operation(summary = "Trigger GDPR data export (async ZIP)")
    public ResponseEntity<PatientService.GDPRExportResponse> exportGdpr(@AuthenticationPrincipal JwtAuthenticatedPrincipal principal) {
        PatientService.GDPRExportResponse response = patientService.exportGdprData(principal.userId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    public record DocumentUploadResponse(UUID id, String url) {}
}