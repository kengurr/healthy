package com.zdravdom.cms.adapters.inbound.rest;

import com.zdravdom.cms.application.dto.*;
import com.zdravdom.cms.application.service.CmsService;
import com.zdravdom.cms.domain.Service.ServiceCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for service catalog management.
 */
@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services", description = "Service catalog and packages")
public class ServiceController {

    private final CmsService cmsService;

    public ServiceController(CmsService cmsService) {
        this.cmsService = cmsService;
    }

    @GetMapping
    @Operation(summary = "List all services (filter by category, search)")
    public ResponseEntity<ServiceListResponse> getServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ServiceListResponse response = cmsService.getServices(category, search, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service details")
    public ResponseEntity<ServiceResponse> getService(@PathVariable UUID id) {
        ServiceResponse response = cmsService.getServiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/packages")
    @Operation(summary = "Get S/M/L packages for a service")
    public ResponseEntity<List<PackageResponse>> getServicePackages(@PathVariable UUID id) {
        List<PackageResponse> packages = cmsService.getServicePackages(id);
        return ResponseEntity.ok(packages);
    }
}