package com.zdravdom.cms.adapters.inbound.rest;

import com.zdravdom.cms.application.dto.CreatePackageRequest;
import com.zdravdom.cms.application.dto.CreateServiceRequest;
import com.zdravdom.cms.application.dto.PackageResponse;
import com.zdravdom.cms.application.dto.UpdateServiceRequest;
import com.zdravdom.cms.application.dto.ServiceResponse;
import com.zdravdom.cms.application.service.AdminCmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin endpoints for CMS — service and package CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminCmsController {

    private final AdminCmsService adminCmsService;

    public AdminCmsController(AdminCmsService adminCmsService) {
        this.adminCmsService = adminCmsService;
    }

    @PostMapping("/services")
    @Operation(summary = "Create a new service")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Service created"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ServiceResponse> createService(@RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCmsService.createService(request));
    }

    @PutMapping("/services/{uuid}")
    @Operation(summary = "Update a service")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service updated"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable UUID uuid,
            @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(adminCmsService.updateService(uuid, request));
    }

    @DeleteMapping("/services/{uuid}")
    @Operation(summary = "Soft-delete a service")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Service deleted"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<Void> deleteService(@PathVariable UUID uuid) {
        adminCmsService.deleteService(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/packages")
    @Operation(summary = "Create a new service package")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Package created"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<PackageResponse> createPackage(@RequestBody CreatePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCmsService.createPackage(request));
    }

    @DeleteMapping("/packages/{id}")
    @Operation(summary = "Soft-delete a service package")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Package deleted"),
        @ApiResponse(responseCode = "404", description = "Package not found")
    })
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        adminCmsService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}