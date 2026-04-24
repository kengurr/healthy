package com.zdravdom.cms.application.dto;

import com.zdravdom.cms.domain.ServicePackage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request to create a new service package.
 */
public record CreatePackageRequest(
    String name,
    ServicePackage.PackageSize size,
    String description,
    List<Long> serviceIds,
    BigDecimal price,
    BigDecimal discountPercent,
    Integer validityDays,
    List<String> benefits
) {}