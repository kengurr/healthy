package com.zdravdom.cms.application.service;

import com.zdravdom.cms.adapters.out.persistence.ServicePackageRepository;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.cms.application.dto.PackageResponse;
import com.zdravdom.cms.application.dto.ServiceListResponse;
import com.zdravdom.cms.application.dto.ServiceResponse;
import com.zdravdom.cms.domain.ServicePackage;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for CMS (Content Management System) - services, packages, pricing.
 */
@Service
public class CmsService {

    private static final Logger log = LoggerFactory.getLogger(CmsService.class);

    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;

    public CmsService(ServiceRepository serviceRepository, ServicePackageRepository packageRepository) {
        this.serviceRepository = serviceRepository;
        this.packageRepository = packageRepository;
    }

    @Transactional(readOnly = true)
    public ServiceListResponse getServices(
            com.zdravdom.cms.domain.Service.ServiceCategory category,
            String search, int page, int size) {
        log.info("Fetching services - category: {}, search: {}, page: {}", category, search, page);

        List<? extends com.zdravdom.cms.domain.Service> services;
        if (category != null && search != null && !search.isBlank()) {
            services = serviceRepository.findByCategory(category).stream()
                .filter(s -> s.getName().toLowerCase().contains(search.toLowerCase()))
                .toList();
        } else if (category != null) {
            services = serviceRepository.findByCategory(category);
        } else if (search != null && !search.isBlank()) {
            services = serviceRepository.findByNameContainingIgnoreCase(search);
        } else {
            services = serviceRepository.findByActiveTrue();
        }

        List<ServiceResponse> content = services.stream()
            .map(this::toResponse)
            .toList();

        return new ServiceListResponse(content, page, size, content.size(),
            (int) Math.ceil((double) content.size() / size));
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(UUID serviceId) {
        log.info("Fetching service: {}", serviceId);
        com.zdravdom.cms.domain.Service service = serviceRepository.findByUuid(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
        return toResponse(service);
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getServicePackages(UUID serviceId) {
        log.info("Fetching packages for service: {}", serviceId);
        if (serviceId == null) {
            return packageRepository.findByActiveTrue().stream()
                .map(this::toPackageResponse)
                .toList();
        }
        // Resolve service UUID → Long database PK, then find packages by that ID
        Long servicePk = serviceRepository.findByUuid(serviceId)
            .map(com.zdravdom.cms.domain.Service::getId)
            .orElse(null);

        List<ServicePackage> packages = servicePk != null
            ? packageRepository.findByServiceId(servicePk)
            : List.of();

        return packages.stream()
            .map(this::toPackageResponse)
            .toList();
    }

    // ─── Response mapping ────────────────────────────────────────────────────

    private ServiceResponse toResponse(com.zdravdom.cms.domain.Service service) {
        return new ServiceResponse(
            service.getId(),
            service.getName(),
            service.getCategory(),
            service.getDescription(),
            service.getDurationMinutes(),
            service.getPrice(),
            service.getRating(),
            service.getImageUrl(),
            service.getIncludedItems() != null ? List.of(service.getIncludedItems()) : List.of()
        );
    }

    private PackageResponse toPackageResponse(ServicePackage pkg) {
        return new PackageResponse(
            pkg.getId(),
            null,
            pkg.getName(),
            pkg.getSize(),
            pkg.getDescription(),
            pkg.getPrice(),
            pkg.getDiscountPercent(),
            pkg.getValidityDays(),
            pkg.getBenefits() != null ? List.of(pkg.getBenefits()) : List.of()
        );
    }
}
