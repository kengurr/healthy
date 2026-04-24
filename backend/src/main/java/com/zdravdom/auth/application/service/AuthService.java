package com.zdravdom.auth.application.service;

import com.zdravdom.auth.adapters.inbound.dto.LoginRequest;
import com.zdravdom.auth.adapters.inbound.dto.ProviderRegisterRequest;
import com.zdravdom.auth.adapters.inbound.dto.RefreshRequest;
import com.zdravdom.auth.adapters.inbound.dto.RegisterRequest;
import com.zdravdom.auth.adapters.out.persistence.AuthTokenRepository;
import com.zdravdom.auth.adapters.out.persistence.UserRepository;
import com.zdravdom.auth.adapters.out.security.JwtTokenProvider;
import com.zdravdom.auth.domain.AuthToken;
import com.zdravdom.auth.domain.Role;
import com.zdravdom.auth.domain.User;
import com.zdravdom.global.exception.GlobalExceptionHandler.ConflictException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.Profession;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Authentication service handling registration, login, and token refresh.
 *
 * <p>Production-ready: all operations hit the real database. Registration
 * creates both the auth.User and the corresponding user.Patient or user.Provider
 * profile in a single transaction.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            PatientRepository patientRepository,
            ProviderRepository providerRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new patient. Creates auth.User + user.Patient in one transaction.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setCredentialsExpired(false);
        user.setMfaEnabled(false);

        user = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setEmail(request.email());
        patient.setPhone(request.phone());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setActive(true);
        patientRepository.save(patient);

        log.info("Registered new patient: {} (userId={})", user.getEmail(), user.getId());

        return buildAuthResponse(user);
    }

    /**
     * Register a new provider. Creates auth.User + user.Provider in one transaction.
     */
    @Transactional
    public AuthResponse registerProvider(ProviderRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        Role role = request.role() != null ? request.role() : Role.PROVIDER;

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setCredentialsExpired(false);
        user.setMfaEnabled(false);

        user = userRepository.save(user);

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setEmail(request.email());
        provider.setPhone(request.phone());
        provider.setFirstName(request.firstName());
        provider.setLastName(request.lastName());
        provider.setRole(role);
        provider.setProfession(parseProfession(request.profession()));
        provider.setStatus(ProviderStatus.PENDING_VERIFICATION);
        provider.setLanguages(new Language[]{Language.SLOVENIAN}); // DEVELOPMENT: Hardcoded default; production should read from request locale or user profile
        providerRepository.save(provider);

        log.info("Registered new provider: {} (userId={}, role={})",
            user.getEmail(), user.getId(), role);

        return buildAuthResponse(user);
    }

    /**
     * Authenticate a user with email and password.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ValidationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new ValidationException("Account is disabled");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    /**
     * Refresh access token using a valid refresh token.
     * Implements refresh token rotation: old token is revoked, new pair is issued.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new ValidationException("Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());

        AuthToken storedToken = authTokenRepository
            .findByRefreshTokenAndRevokedFalse(request.refreshToken())
            .orElseThrow(() -> new ValidationException("Refresh token not found"));

        if (storedToken.isExpired()) {
            throw new ValidationException("Refresh token has expired");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isActive()) {
            throw new ValidationException("Account is disabled");
        }

        // Revoke old refresh token (rotation)
        storedToken.setRevoked(true);
        authTokenRepository.save(storedToken);

        log.debug("Refresh token rotated for userId: {}", userId);

        return buildAuthResponseWithNewRefresh(user, storedToken.getDeviceInfo());
    }

    private AuthResponse buildAuthResponse(User user) {
        return buildAuthResponseWithNewRefresh(user, "web");
    }

    private AuthResponse buildAuthResponseWithNewRefresh(User user, String deviceInfo) {
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken, deviceInfo);

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtTokenProvider.getAccessTokenExpirySeconds()
        );
    }

    private void saveRefreshToken(Long userId, String refreshToken, String deviceInfo) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS); // DEVELOPMENT: 7-day fixed; production should be configurable (env var or user preference)

        AuthToken authToken = new AuthToken(null, refreshToken, deviceInfo, null, expiresAt);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        authToken.setUser(user);
        authTokenRepository.save(authToken);
    }

    private Profession parseProfession(String profession) {
        if (profession == null || profession.isBlank()) {
            return null;
        }
        try {
            return Profession.valueOf(profession.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                "Invalid profession: '" + profession + "'. Valid values are: NURSE, PHYSIOTHERAPIST, DOCTOR, CAREGIVER, SOCIAL_WORKER");
        }
    }

    // Response DTO (returned to client, not a request)

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
    ) {}
}
