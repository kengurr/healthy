package com.zdravdom.cms.application.dto;

import java.util.List;

/**
 * Paginated service list response.
 */
public record ServiceListResponse(
    List<ServiceResponse> content,
    int page,
    int size,
    long total,
    int totalPages
) {}