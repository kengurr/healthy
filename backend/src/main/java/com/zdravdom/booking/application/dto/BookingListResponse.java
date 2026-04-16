package com.zdravdom.booking.application.dto;

import java.util.List;

/**
 * Paginated booking list response.
 */
public record BookingListResponse(
    List<BookingResponse> content,
    int page,
    int size,
    long total,
    int totalPages
) {}