package com.zdravdom.user.adapters.inbound.rest;

import com.zdravdom.user.application.service.AdminUserService;
import com.zdravdom.user.application.dto.AdminPatientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for user (patient) management.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List all patients", description = "Returns all registered patients")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient list"),
        @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN, SUPERADMIN only")
    })
    public ResponseEntity<List<AdminPatientResponse>> listPatients() {
        return ResponseEntity.ok(adminUserService.getAllPatients());
    }
}
