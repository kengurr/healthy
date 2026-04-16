package com.zdravdom.cms.application.service;

import com.zdravdom.cms.application.dto.*;
import com.zdravdom.cms.domain.Service.ServiceCategory;
import com.zdravdom.cms.domain.ServicePackage;
import com.zdravdom.cms.domain.ServicePackage.PackageSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for CMS (Content Management System) - services, packages, pricing.
 */
@Service
public class CmsService {

    private static final Logger log = LoggerFactory.getLogger(CmsService.class);

    @Transactional(readOnly = true)
    public ServiceListResponse getServices(ServiceCategory category, String search, int page, int size) {
        log.info("Fetching services - category: {}, search: {}, page: {}", category, search, page);

        List<ServiceResponse> services = List.of(
            new ServiceResponse(
                1L, "Home Nursing Care", ServiceCategory.NURSING_CARE,
                "Professional nursing care at home including wound dressing, medication management, and health monitoring.",
                60, BigDecimal.valueOf(45.00), 4.8,
                "https://s3.zdravdom.com/services/nursing.jpg",
                List.of("Wound care", "Medication administration", "Vital signs monitoring", "Health education")
            ),
            new ServiceResponse(
                2L, "Physiotherapy Session", ServiceCategory.PHYSIOTHERAPY,
                "Expert physiotherapy for rehabilitation, pain management, and mobility improvement.",
                45, BigDecimal.valueOf(55.00), 4.9,
                "https://s3.zdravdom.com/services/physio.jpg",
                List.of("Movement assessment", "Exercise therapy", "Pain management", "Mobility training")
            ),
            new ServiceResponse(
                3L, "Medical Consultation", ServiceCategory.MEDICAL_CONSULTATION,
                "Doctor consultation for diagnosis, treatment planning, and medical advice.",
                30, BigDecimal.valueOf(70.00), 4.7,
                "https://s3.zdravdom.com/services/consultation.jpg",
                List.of("Health assessment", "Treatment planning", "Medical advice", "Referral coordination")
            ),
            new ServiceResponse(
                4L, "Wound Care Specialist", ServiceCategory.WOUND_CARE,
                "Specialized wound care for chronic wounds, post-surgical care, and pressure ulcers.",
                45, BigDecimal.valueOf(50.00), 4.6,
                "https://s3.zdravdom.com/services/wound.jpg",
                List.of("Wound assessment", "Dressing changes", "Infection management", "Healing monitoring")
            ),
            new ServiceResponse(
                5L, "Elderly Care Package", ServiceCategory.ELDERLY_CARE,
                "Comprehensive care for elderly patients including daily living assistance and health monitoring.",
                120, BigDecimal.valueOf(80.00), 4.8,
                "https://s3.zdravdom.com/services/elderly.jpg",
                List.of("Personal care", "Mobility assistance", "Meal preparation", "Medication reminders")
            )
        );

        return new ServiceListResponse(services, page, size, services.size(), 1);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(UUID serviceId) {
        log.info("Fetching service: {}", serviceId);
        return new ServiceResponse(
            toLong(serviceId), "Home Nursing Care", ServiceCategory.NURSING_CARE,
            "Professional nursing care at home including wound dressing, medication management, and health monitoring.",
            60, BigDecimal.valueOf(45.00), 4.8,
            "https://s3.zdravdom.com/services/nursing.jpg",
            List.of("Wound care", "Medication administration", "Vital signs monitoring", "Health education")
        );
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getServicePackages(UUID serviceId) {
        log.info("Fetching packages for service: {}", serviceId);

        return List.of(
            new PackageResponse(
                1L, toLong(serviceId), "Basic Package", PackageSize.S,
                "Perfect for occasional care needs",
                BigDecimal.valueOf(120.00), BigDecimal.valueOf(10),
                30, List.of("3 visits", "Basic care", "Phone support")
            ),
            new PackageResponse(
                2L, toLong(serviceId), "Standard Package", PackageSize.M,
                "Ideal for ongoing care requirements",
                BigDecimal.valueOf(350.00), BigDecimal.valueOf(15),
                60, List.of("8 visits", "Priority booking", "24/7 support", "Care plan included")
            ),
            new PackageResponse(
                3L, toLong(serviceId), "Premium Package", PackageSize.L,
                "Comprehensive care for long-term needs",
                BigDecimal.valueOf(800.00), BigDecimal.valueOf(20),
                90, List.of("12 visits", "Dedicated provider", "Priority support", "Free follow-up consultations")
            )
        );
    }

    private Long toLong(UUID uuid) {
        return uuid != null ? uuid.getMostSignificantBits() : null;
    }
}
