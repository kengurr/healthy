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
        log.info("Registered new patient: {}", user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken, "web");

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

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : Role.PROVIDER);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setCredentialsExpired(false);
        user.setMfaEnabled(false);

        user = userRepository.save(user);
        log.info("Registered new provider: {} with role {}", user.getEmail(), user.getRole());

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken, "web");

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

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new ValidationException("Account is disabled");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken, "web");

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

        // Revoke old refresh token
        storedToken.setRevoked(true);
        authTokenRepository.save(storedToken);

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), newRefreshToken, storedToken.getDeviceInfo());

        return new AuthResponse(
            accessToken,
            newRefreshToken,
            jwtTokenProvider.getAccessTokenExpirySeconds()
        );
    }

    private void saveRefreshToken(Long userId, String refreshToken, String deviceInfo) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        AuthToken authToken = new AuthToken(
            user, refreshToken, deviceInfo, null, expiresAt
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
