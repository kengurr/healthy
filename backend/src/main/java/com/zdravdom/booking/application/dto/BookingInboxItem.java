package com.zdravdom.booking.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Booking inbox item for provider dashboard.
 */
public record BookingInboxItem(
    Long id,
    Long patientId,
    String patientFirstName,
    String patientLastName,
    String serviceName,
    AddressSummary address,
    LocalDate date,
    String timeSlot,
    com.zdravdom.booking.domain.Booking.BookingStatus status
) {
    public record AddressSummary(
        String street,
        String city,
        String postalCode
    ) {}
}