package com.zdravdom.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

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
    String region,
    String country,
    BigDecimal latitude,
    BigDecimal longitude,
    String instructions,
    boolean isDefault
) {}
