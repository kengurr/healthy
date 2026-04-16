package com.zdravdom.cms.application.dto;

import com.zdravdom.cms.domain.Service.ServiceCategory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service response DTO.
 */
public record ServiceResponse(
    Long id,
    String name,
    ServiceCategory category,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    Double rating,
    String imageUrl,
    List<String> includedItems
) {}