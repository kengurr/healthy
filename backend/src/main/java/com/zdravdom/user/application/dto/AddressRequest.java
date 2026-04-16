package com.zdravdom.user.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Address request DTO.
 */
public record AddressRequest(
    @NotBlank String label,
    @NotBlank String street,
    String houseNumber,
    String apartmentNumber,
    @NotBlank String city,
    @NotBlank String postalCode,
    String country,
    Double latitude,
    Double longitude,
    String instructions,
    boolean isDefault
) {}