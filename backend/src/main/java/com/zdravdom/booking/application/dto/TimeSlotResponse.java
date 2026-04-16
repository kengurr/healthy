package com.zdravdom.booking.application.dto;

import java.time.LocalTime;

/**
 * Time slot response DTO.
 */
public record TimeSlotResponse(
    LocalTime startTime,
    LocalTime endTime,
    boolean available
) {}