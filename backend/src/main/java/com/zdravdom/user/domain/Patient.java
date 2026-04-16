package com.zdravdom.user.domain;

import com.zdravdom.auth.domain.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient profile information.
 * Maps to user.patients table.
 */
@Entity
@Table(name = "patients", schema = "`user`")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "insuranceProvider", column = @Column(name = "insurance_provider")),
        @AttributeOverride(name = "policyNumber", column = @Column(name = "policy_number")),
        @AttributeOverride(name = "groupNumber", column = @Column(name = "group_number"))
    })
    private InsuranceDetails insuranceDetails;

    @Column(name = "allergies", columnDefinition = "TEXT[]")
    private String[] allergies;

    @Column(name = "chronic_conditions", columnDefinition = "TEXT[]")
    private String[] chronicConditions;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "emergency_contact_name")),
        @AttributeOverride(name = "phone", column = @Column(name = "emergency_contact_phone")),
        @AttributeOverride(name = "relationship", column = @Column(name = "emergency_contact_relationship"))
    })
    private EmergencyContact emergencyContact;

    // Address fields embedded directly in patient (no separate Address entity needed for MVP)
    private String addressStreet;
    private String addressHouseNumber;
    private String addressApartmentNumber;
    private String addressCity;
    private String addressPostalCode;
    private String addressRegion;
    private String addressCountry;
    private String addressInstructions;

    private boolean verified = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    @Embeddable
    public static class InsuranceDetails {
        @Column(name = "insurance_provider")
        private String insuranceProvider;
        @Column(name = "policy_number")
        private String policyNumber;
        @Column(name = "group_number")
        private String groupNumber;

        public InsuranceDetails() {}

        public InsuranceDetails(String insuranceProvider, String policyNumber, String groupNumber) {
            this.insuranceProvider = insuranceProvider;
            this.policyNumber = policyNumber;
            this.groupNumber = groupNumber;
        }

        public String getInsuranceProvider() { return insuranceProvider; }
        public String getPolicyNumber() { return policyNumber; }
        public String getGroupNumber() { return groupNumber; }
        public void setInsuranceProvider(String v) { this.insuranceProvider = v; }
        public void setPolicyNumber(String v) { this.policyNumber = v; }
        public void setGroupNumber(String v) { this.groupNumber = v; }
    }

    @Embeddable
    public static class EmergencyContact {
        private String name;
        private String phone;
        private String relationship;

        public EmergencyContact() {}

        public EmergencyContact(String name, String phone, String relationship) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getRelationship() { return relationship; }
        public void setName(String v) { this.name = v; }
        public void setPhone(String v) { this.phone = v; }
        public void setRelationship(String v) { this.relationship = v; }
    }

    // Default constructor for JPA
    public Patient() {}

    // Convenience constructor matching old record signature (for mock services)
    public Patient(Long id, String email, String phone, String firstName, String lastName,
                  LocalDate dateOfBirth, Gender gender, InsuranceDetails insuranceDetails,
                  java.util.List<String> allergies, java.util.List<String> chronicConditions,
                  EmergencyContact emergencyContact, String addressStreet, String addressHouseNumber,
                  String addressApartmentNumber, String addressCity, String addressPostalCode,
                  String addressRegion, String addressCountry, String addressInstructions,
                  LocalDateTime createdAt, LocalDateTime updatedAt,
                  boolean verified, boolean active) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.insuranceDetails = insuranceDetails;
        this.allergies = allergies != null ? allergies.toArray(new String[0]) : new String[0];
        this.chronicConditions = chronicConditions != null ? chronicConditions.toArray(new String[0]) : new String[0];
        this.emergencyContact = emergencyContact;
        this.addressStreet = addressStreet;
        this.addressHouseNumber = addressHouseNumber;
        this.addressApartmentNumber = addressApartmentNumber;
        this.addressCity = addressCity;
        this.addressPostalCode = addressPostalCode;
        this.addressRegion = addressRegion;
        this.addressCountry = addressCountry;
        this.addressInstructions = addressInstructions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.verified = verified;
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Transient Address object for service layer compatibility
    @Transient
    public Address getAddress() {
        Address addr = new Address();
        addr.setStreet(addressStreet != null ? addressStreet : "");
        addr.setHouseNumber(addressHouseNumber);
        addr.setApartmentNumber(addressApartmentNumber);
        addr.setCity(addressCity != null ? addressCity : "");
        addr.setPostalCode(addressPostalCode != null ? addressPostalCode : "");
        addr.setRegion(addressRegion);
        addr.setCountry(addressCountry != null ? addressCountry : "SI");
        addr.setInstructions(addressInstructions);
        return addr;
    }

    // Getters for address fields
    public String getAddressStreet() { return addressStreet; }
    public String getAddressHouseNumber() { return addressHouseNumber; }
    public String getAddressApartmentNumber() { return addressApartmentNumber; }
    public String getAddressCity() { return addressCity; }
    public String getAddressPostalCode() { return addressPostalCode; }
    public String getAddressRegion() { return addressRegion; }
    public String getAddressCountry() { return addressCountry; }
    public String getAddressInstructions() { return addressInstructions; }

    // Setters for address fields
    public void setAddressStreet(String v) { this.addressStreet = v; }
    public void setAddressHouseNumber(String v) { this.addressHouseNumber = v; }
    public void setAddressApartmentNumber(String v) { this.addressApartmentNumber = v; }
    public void setAddressCity(String v) { this.addressCity = v; }
    public void setAddressPostalCode(String v) { this.addressPostalCode = v; }
    public void setAddressRegion(String v) { this.addressRegion = v; }
    public void setAddressCountry(String v) { this.addressCountry = v; }
    public void setAddressInstructions(String v) { this.addressInstructions = v; }

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public InsuranceDetails getInsuranceDetails() { return insuranceDetails; }
    public String[] getAllergies() { return allergies; }
    public String[] getChronicConditions() { return chronicConditions; }
    public EmergencyContact getEmergencyContact() { return emergencyContact; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isVerified() { return verified; }
    public boolean isActive() { return active; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setGender(Gender gender) { this.gender = gender; }
    public void setInsuranceDetails(InsuranceDetails insuranceDetails) { this.insuranceDetails = insuranceDetails; }
    public void setAllergies(String[] allergies) { this.allergies = allergies; }
    public void setChronicConditions(String[] chronicConditions) { this.chronicConditions = chronicConditions; }
    public void setEmergencyContact(EmergencyContact emergencyContact) { this.emergencyContact = emergencyContact; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public void setActive(boolean active) { this.active = active; }
}
