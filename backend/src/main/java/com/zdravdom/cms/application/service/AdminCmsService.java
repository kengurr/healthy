package com.zdravdom.cms.application.service;

import com.zdravdom.cms.adapters.out.persistence.ServicePackageRepository;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.cms.application.dto.CreatePackageRequest;
import com.zdravdom.cms.application.dto.CreateServiceRequest;
import com.zdravdom.cms.application.dto.UpdateServiceRequest;
import com.zdravdom.cms.application.dto.ServiceResponse;
import com.zdravdom.cms.application.dto.PackageResponse;
import com.zdravdom.cms.domain.ServicePackage;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Admin service for CMS write operations — create/update/delete services and packages.
 */
@Service
public class AdminCmsService {

    private static final Logger log = LoggerFactory.getLogger(AdminCmsService.class);

    private final ServiceRepository serviceRepository;
    private final ServicePackageRepository packageRepository;

    public AdminCmsService(ServiceRepository serviceRepository, ServicePackageRepository packageRepository) {
        this.serviceRepository = serviceRepository;
        this.packageRepository = packageRepository;
    }

    @Transactional
    public ServiceResponse createService(CreateServiceRequest request) {
        com.zdravdom.cms.domain.Service service = com.zdravdom.cms.domain.Service.create();
        service.setName(request.name());
        service.setCategory(request.category());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        service.setImageUrl(request.imageUrl());
        service.setIncludedItems(request.includedItems() != null ? request.includedItems().toArray(new String[0]) : null);
        service.setRequiredDocuments(request.requiredDocuments() != null ? request.requiredDocuments().toArray(new String[0]) : null);
        service.setActive(true);
        service = serviceRepository.save(service);
        log.info("Created service: {} ({})", service.getName(), service.getId());
        return toServiceResponse(service);
    }

    @Transactional
    public ServiceResponse updateService(UUID uuid, UpdateServiceRequest request) {
        com.zdravdom.cms.domain.Service service = serviceRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResourceNotFoundException("Service", uuid));
        if (request.name() != null) service.setName(request.name());
        if (request.category() != null) service.setCategory(request.category());
        if (request.description() != null) service.setDescription(request.description());
        if (request.durationMinutes() != null) service.setDurationMinutes(request.durationMinutes());
        if (request.price() != null) service.setPrice(request.price());
        if (request.imageUrl() != null) service.setImageUrl(request.imageUrl());
        if (request.includedItems() != null) service.setIncludedItems(request.includedItems().toArray(new String[0]));
        if (request.requiredDocuments() != null) service.setRequiredDocuments(request.requiredDocuments().toArray(new String[0]));
        if (request.active() != null) service.setActive(request.active());
        service = serviceRepository.save(service);
        log.info("Updated service: {}", uuid);
        return toServiceResponse(service);
    }

    @Transactional
    public void deleteService(UUID uuid) {
        com.zdravdom.cms.domain.Service service = serviceRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResourceNotFoundException("Service", uuid));
        service.setActive(false);
        serviceRepository.save(service);
        log.info("Soft-deleted service: {}", uuid);
    }

    @Transactional
    public PackageResponse createPackage(CreatePackageRequest request) {
        ServicePackage pkg = ServicePackage.create();
        pkg.setName(request.name());
        pkg.setSize(request.size());
        pkg.setDescription(request.description());
        pkg.setServiceIds(request.serviceIds() != null ? request.serviceIds().toArray(new Long[0]) : null);
        pkg.setPrice(request.price());
        pkg.setDiscountPercent(request.discountPercent() != null ? request.discountPercent() : BigDecimal.ZERO);
        pkg.setValidityDays(request.validityDays());
        pkg.setBenefits(request.benefits() != null ? request.benefits().toArray(new String[0]) : null);
        pkg.setActive(true);
        pkg = packageRepository.save(pkg);
        log.info("Created package: {} ({})", pkg.getName(), pkg.getId());
        return toPackageResponse(pkg);
    }

    @Transactional
    public void deletePackage(Long id) {
        ServicePackage pkg = packageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Package", id));
        pkg.setActive(false);
        packageRepository.save(pkg);
        log.info("Soft-deleted package: {}", id);
    }

    private ServiceResponse toServiceResponse(com.zdravdom.cms.domain.Service service) {
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