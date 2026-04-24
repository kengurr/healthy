package com.zdravdom.cms.application.dto;

import com.zdravdom.cms.domain.Service;
import com.zdravdom.cms.domain.Service.ServiceCategory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request to update an existing service.
 */
public record UpdateServiceRequest(
    String name,
    ServiceCategory category,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    String imageUrl,
    List<String> includedItems,
    List<String> requiredDocuments,
    Boolean active
) {}