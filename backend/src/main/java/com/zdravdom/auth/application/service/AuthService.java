package com.zdravdom.auth.application.service;

import com.zdravdom.auth.adapters.out.persistence.AuthTokenRepository;
import com.zdravdom.auth.adapters.out.persistence.UserRepository;
import com.zdravdom.auth.adapters.out.security.JwtTokenProvider;
import com.zdravdom.auth.domain.AuthToken;
import com.zdravdom.auth.domain.Role;
import com.zdravdom.auth.domain.User;
import com.zdravdom.global.exception.GlobalExceptionHandler.ConflictException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Authentication service handling registration, login, and token refresh.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       AuthTokenRepository authTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        validatePassword(request.password());

        User user = new User(
            null,
            request.email(),
            passwordEncoder.encode(request.password()),
            Role.PATIENT,
            false,
            false,
            false,
            false,
            Instant.now(),
            Instant.now(),
            null
        );

        user = userRepository.save(user);
        log.info("Registered new patient: {}", user.email());

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.id(), user.email(), user.role().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id());

        saveRefreshToken(user.id(), refreshToken, "web");

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtTokenProvider.getAccessTokenExpirySeconds()
        );
    }

    @Transactional
    public AuthResponse registerProvider(ProviderRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        validatePassword(request.password());

        User user = new User(
            null,
            request.email(),
            passwordEncoder.encode(request.password()),
            request.role() != null ? request.role() : Role.PROVIDER,
            false,
            false,
            false,
            false,
            Instant.now(),
            Instant.now(),
            null
        );

        user = userRepository.save(user);
        log.info("Registered new provider: {} with role {}", user.email(), user.role());

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.id(), user.email(), user.role().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id());

        saveRefreshToken(user.id(), refreshToken, "web");

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtTokenProvider.getAccessTokenExpirySeconds()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ValidationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ValidationException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new ValidationException("Account is disabled");
        }

        user = new User(
            user.id(), user.email(), user.passwordHash(), user.role(),
            user.mfaEnabled(), user.accountLocked(), user.accountExpired(),
            user.credentialsExpired(), user.createdAt(), Instant.now(), Instant.now()
        );
        userRepository.save(user);

        log.info("User logged in: {}", user.email());

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.id(), user.email(), user.role().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id());

        saveRefreshToken(user.id(), refreshToken, "web");

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtTokenProvider.getAccessTokenExpirySeconds()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new ValidationException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());

        AuthToken storedToken = authTokenRepository
            .findByRefreshTokenAndRevokedFalse(request.refreshToken())
            .orElseThrow(() -> new ValidationException("Refresh token not found"));

        if (storedToken.isExpired()) {
            throw new ValidationException("Refresh token has expired");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isEnabled()) {
            throw new ValidationException("Account is disabled");
        }

        // Revoke old refresh token
        AuthToken revokedToken = new AuthToken(
            storedToken.id(), storedToken.userId(), storedToken.refreshToken(),
            storedToken.deviceInfo(), storedToken.ipAddress(),
            storedToken.issuedAt(), storedToken.expiresAt(), true
        );
        authTokenRepository.save(revokedToken);

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.id(), user.email(), user.role().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.id());

        saveRefreshToken(user.id(), newRefreshToken, storedToken.deviceInfo());

        return new AuthResponse(
            accessToken,
            newRefreshToken,
            jwtTokenProvider.getAccessTokenExpirySeconds()
        );
    }

    private void saveRefreshToken(UUID userId, String refreshToken, String deviceInfo) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        AuthToken authToken = new AuthToken(
            null, userId, refreshToken, deviceInfo, null, now, expiresAt, false
        );
        authTokenRepository.save(authToken);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
    }

    // Request/Response DTOs

    public record RegisterRequest(
        String email,
        String phone,
        String password,
        String firstName,
        String lastName,
        java.time.LocalDate dateOfBirth
    ) {}

    public record ProviderRegisterRequest(
        String email,
        String phone,
        String password,
        Role role,
        String profession,
        String firstName,
        String lastName
    ) {}

    public record LoginRequest(
        String email,
        String password
    ) {}

    public record RefreshRequest(
        String refreshToken
    ) {}

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
    ) {}
}