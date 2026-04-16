package com.zdravdom.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * User entity representing an authenticated user in the system.
 * Maps to auth.users table.
 */
@Entity
@Table(name = "users", schema = "auth")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "account_locked")
    private boolean accountLocked = false;

    @Column(name = "account_expired")
    private boolean accountExpired = false;

    @Column(name = "credentials_expired")
    private boolean credentialsExpired = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // Default constructor for JPA
    public User() {}

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Domain logic preserved from original record
    public boolean isActive() {
        return !accountLocked && !accountExpired && !credentialsExpired && enabled;
    }

    public boolean isProvider() {
        return role == Role.PROVIDER;
    }

    public boolean isPatient() {
        return role == Role.PATIENT;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SUPERADMIN;
    }

    // Getters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public String getMfaSecret() { return mfaSecret; }
    public boolean isAccountLocked() { return accountLocked; }
    public boolean isAccountExpired() { return accountExpired; }
    public boolean isCredentialsExpired() { return credentialsExpired; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(Role role) { this.role = role; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }
    public void setAccountExpired(boolean accountExpired) { this.accountExpired = accountExpired; }
    public void setCredentialsExpired(boolean credentialsExpired) { this.credentialsExpired = credentialsExpired; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
