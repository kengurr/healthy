package com.zdravdom.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Address entity for patient and visit locations.
 * Maps to user.addresses table.
 */
@Entity
@Table(name = "addresses", schema = "`user`")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(nullable = false)
    private String street;

    @Column(name = "house_number")
    private String houseNumber;

    @Column(name = "apartment_number")
    private String apartmentNumber;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    private String region;

    @Column(nullable = false)
    private String country = "SI";

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "is_primary")
    private boolean isPrimary = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (country == null) country = "SI";
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

    /** Factory method for application-layer instantiation. */
    public static Address create() {
        return new Address();
    }
}
