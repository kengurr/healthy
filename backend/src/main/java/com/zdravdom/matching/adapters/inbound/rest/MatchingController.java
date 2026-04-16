package com.zdravdom.matching.adapters.inbound.rest;

import com.zdravdom.matching.application.dto.ProviderSummary;
import com.zdravdom.matching.application.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for provider matching.
 */
@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Provider matching and search")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping
    @Operation(summary = "Match providers by service, date, time and address")
    public ResponseEntity<List<ProviderSummary>> matchProviders(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam String time,
            @RequestParam UUID addressId) {
        List<ProviderSummary> providers = matchingService.matchProviders(
            serviceId, date, time, addressId);
        return ResponseEntity.ok(providers);
    }
}