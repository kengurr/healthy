package com.zdravdom.user.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Address entity for patient and visit locations.
 * Maps to user.addresses table.
 */
@Entity
@Table(name = "addresses", schema = "`user`")
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

    // Default constructor for JPA
    public Address() {}

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

    // Getters
    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Long getProviderId() { return providerId; }
    public String getStreet() { return street; }
    public String getHouseNumber() { return houseNumber; }
    public String getApartmentNumber() { return apartmentNumber; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public String getRegion() { return region; }
    public String getCountry() { return country; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getInstructions() { return instructions; }
    public boolean isPrimary() { return isPrimary; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public void setStreet(String street) { this.street = street; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }
    public void setApartmentNumber(String apartmentNumber) { this.apartmentNumber = apartmentNumber; }
    public void setCity(String city) { this.city = city; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setRegion(String region) { this.region = region; }
    public void setCountry(String country) { this.country = country; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setPrimary(boolean isPrimary) { this.isPrimary = isPrimary; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
