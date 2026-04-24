package com.zdravdom.analytics.adapters.inbound.rest;

import com.zdravdom.analytics.application.AdminAnalyticsService;
import com.zdravdom.analytics.application.dto.AdminDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin analytics endpoints for operations dashboard.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Operations dashboard KPIs", description = "Returns aggregate statistics for the admin dashboard")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard KPIs"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN or SUPERADMIN role")
    })
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }
}