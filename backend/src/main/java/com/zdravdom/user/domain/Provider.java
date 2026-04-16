package com.zdravdom.user.domain;

import com.zdravdom.auth.domain.Role;
import com.zdravdom.auth.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Healthcare provider profile information.
 * Maps to user.providers table.
 */
@Entity
@Table(name = "providers", schema = "`user`")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Profession profession;

    @Enumerated(EnumType.STRING)
    private Specialty specialty;

    private Double rating = 0.0;

    @Column(name = "reviews_count")
    private Integer reviewsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT[]")
    private Language[] languages;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private ProviderStatus status = ProviderStatus.PENDING_VERIFICATION;

    private boolean verified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Profession {
        NURSE, PHYSIOTHERAPIST, DOCTOR, CAREGIVER, SOCIAL_WORKER
    }

    public enum Specialty {
        GENERAL_CARE, WOUND_CARE, POST_SURGERY_CARE, ELDERLY_CARE,
        PEDIATRIC_CARE, CHRONIC_DISEASE_MANAGEMENT, REHABILITATION,
        PALLIATIVE_CARE, MENTAL_HEALTH
    }

    public enum Language {
        SLOVENIAN, ENGLISH, GERMAN, ITALIAN, CROATIAN, SERBIAN, BOSNIAN, HUNGARIAN
    }

    public enum ProviderStatus {
        PENDING_VERIFICATION, ACTIVE, SUSPENDED, INACTIVE
    }

    // Default constructor for JPA
    public Provider() {}

    // Convenience constructor matching mock service call pattern
    public Provider(Long id, String firstName, String lastName, String email, String phone,
                    Role role, Profession profession, Specialty specialty,
                    Double rating, Integer reviewsCount, java.util.List<Language> languages,
                    Integer yearsOfExperience, String bio, String photoUrl,
                    ProviderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                    boolean verified) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.profession = profession;
        this.specialty = specialty;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
        this.languages = languages != null ? languages.toArray(new Language[0]) : new Language[0];
        this.yearsOfExperience = yearsOfExperience;
        this.bio = bio;
        this.photoUrl = photoUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.verified = verified;
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

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public Profession getProfession() { return profession; }
    public Specialty getSpecialty() { return specialty; }
    public Double getRating() { return rating; }
    public Integer getReviewsCount() { return reviewsCount; }
    public Language[] getLanguages() { return languages; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public String getBio() { return bio; }
    public String getPhotoUrl() { return photoUrl; }
    public ProviderStatus getStatus() { return status; }
    public boolean isVerified() { return verified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(Role role) { this.role = role; }
    public void setProfession(Profession profession) { this.profession = profession; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public void setRating(Double rating) { this.rating = rating; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }
    public void setLanguages(Language[] languages) { this.languages = languages; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
    public void setBio(String bio) { this.bio = bio; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setStatus(ProviderStatus status) { this.status = status; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
