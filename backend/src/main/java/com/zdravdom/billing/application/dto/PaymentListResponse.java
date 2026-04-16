package com.zdravdom.billing.application.dto;

import java.util.List;

/**
 * Paginated payment list response.
 */
public record PaymentListResponse(
    List<PaymentResponse> content,
    int page,
    int size,
    long total,
    int totalPages
) {}