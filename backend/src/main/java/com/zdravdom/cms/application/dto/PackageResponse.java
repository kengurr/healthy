package com.zdravdom.cms.application.dto;

import com.zdravdom.cms.domain.ServicePackage.PackageSize;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service package response DTO.
 */
public record PackageResponse(
    Long id,
    Long serviceId,
    String name,
    PackageSize size,
    String description,
    BigDecimal price,
    BigDecimal discountPercent,
    Integer validityDays,
    List<String> benefits
) {}