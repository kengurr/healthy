package com.zdravdom.user.domain;

import java.math.BigDecimal;

/**
 * Address entity for patient and visit locations.
 */
public record Address(
    Long id,
    String street,
    String houseNumber,
    String apartmentNumber,
    String city,
    String postalCode,
    String region,
    String country,
    BigDecimal latitude,
    BigDecimal longitude,
    String instructions
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be null or blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be null or blank");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code cannot be null or blank");
        }
        if (country == null || country.isBlank()) {
            country = "SI"; // Default to Slovenia
        }
    }

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);
        if (houseNumber != null && !houseNumber.isBlank()) {
            sb.append(" ").append(houseNumber);
        }
        if (apartmentNumber != null && !apartmentNumber.isBlank()) {
            sb.append("/").append(apartmentNumber);
        }
        sb.append(", ").append(postalCode).append(" ").append(city);
        return sb.toString();
    }
}
