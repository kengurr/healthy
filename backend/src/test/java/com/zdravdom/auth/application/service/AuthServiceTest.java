package com.zdravdom.auth.application.service;

import com.zdravdom.auth.adapters.inbound.dto.LoginRequest;
import com.zdravdom.auth.adapters.inbound.dto.ProviderRegisterRequest;
import com.zdravdom.auth.adapters.inbound.dto.RefreshRequest;
import com.zdravdom.auth.adapters.inbound.dto.RegisterRequest;
import com.zdravdom.auth.adapters.out.persistence.AuthTokenRepository;
import com.zdravdom.auth.adapters.out.persistence.UserRepository;
import com.zdravdom.auth.adapters.out.security.JwtTokenProvider;
import com.zdravdom.auth.application.service.AuthService.AuthResponse;
import com.zdravdom.auth.domain.AuthToken;
import com.zdravdom.auth.domain.Role;
import com.zdravdom.auth.domain.User;
import com.zdravdom.global.exception.GlobalExceptionHandler.ConflictException;
import com.zdravdom.global.exception.GlobalExceptionHandler.ValidationException;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthTokenRepository authTokenRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            authTokenRepository,
            patientRepository,
            providerRepository,
            jwtTokenProvider,
            passwordEncoder
        );
    }

    // ─── Register (Patient) ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("register() — patient registration")
    class Register {

        @Test
        @DisplayName("saves user and patient, returns tokens")
        void savesUserAndPatient_ReturnsTokens() {
            RegisterRequest request = new RegisterRequest(
                "patient@test.com", "+38612345678", "password123",
                "Janez", "Novak", LocalDate.of(1985, 3, 15)
            );

            when(userRepository.existsByEmail("patient@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtTokenProvider.generateAccessToken(1L, "patient@test.com", "PATIENT"))
                .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L))
                .thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L, "patient@test.com")));

            AuthResponse response = authService.register(request);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.expiresIn()).isEqualTo(900L);

            // Verify User was saved
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo("patient@test.com");
            assertThat(savedUser.getRole()).isEqualTo(Role.PATIENT);
            assertThat(savedUser.isEnabled()).isTrue();
            assertThat(savedUser.isAccountLocked()).isFalse();

            // Verify Patient was saved
            verify(patientRepository).save(any());
        }

        @Test
        @DisplayName("throws ConflictException when email already exists")
        void throwsConflict_WhenEmailExists() {
            RegisterRequest request = new RegisterRequest(
                "exists@test.com", "+38612345678", "password123",
                "Janez", "Novak", null
            );
            when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already exists");

            verify(userRepository, never()).save(any());
            verify(patientRepository, never()).save(any());
        }
    }

    // ─── Register Provider ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerProvider() — provider registration")
    class RegisterProvider {

        @Test
        @DisplayName("saves user and provider with correct role")
        void savesUserAndProvider_ReturnsTokens() {
            ProviderRegisterRequest request = new ProviderRegisterRequest(
                "provider@test.com", "+38698765432", "password123",
                Role.PROVIDER, "NURSE", "Marko", "Horvat"
            );

            when(userRepository.existsByEmail("provider@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(2L);
                return u;
            });
            when(jwtTokenProvider.generateAccessToken(2L, "provider@test.com", "PROVIDER"))
                .thenReturn("provider-access-token");
            when(jwtTokenProvider.generateRefreshToken(2L))
                .thenReturn("provider-refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(makeUser(2L, "provider@test.com")));

            AuthResponse response = authService.registerProvider(request);

            assertThat(response.accessToken()).isEqualTo("provider-access-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.PROVIDER);

            verify(providerRepository).save(any());
        }

        @Test
        @DisplayName("throws ConflictException when email already exists")
        void throwsConflict_WhenEmailExists() {
            ProviderRegisterRequest request = new ProviderRegisterRequest(
                "exists@provider.com", "+38611111111", "password123",
                Role.PROVIDER, "NURSE", "Janez", "Novak"
            );
            when(userRepository.existsByEmail("exists@provider.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerProvider(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already exists");
        }

        @Test
        @DisplayName("throws ValidationException for unknown profession")
        void throwsValidationException_ForUnknownProfession() {
            ProviderRegisterRequest request = new ProviderRegisterRequest(
                "provider@test.com", "+38698765432", "password123",
                null, "UNKNOWN_PROFESSION", "Marko", "Horvat"
            );
            when(userRepository.existsByEmail("provider@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(2L);
                return u;
            });

            assertThatThrownBy(() -> authService.registerProvider(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid profession");
        }
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns tokens on valid credentials")
        void returnsTokens_OnValidCredentials() {
            User user = makeUser(1L, "login@test.com");
            user.setPasswordHash("hashed_password");
            user.setRole(Role.PATIENT);

            when(userRepository.findByEmail("login@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correct_password", "hashed_password")).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(1L, "login@test.com", "PATIENT"))
                .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            AuthResponse response = authService.login(
                new LoginRequest("login@test.com", "correct_password"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("throws ValidationException on wrong password")
        void throwsValidationException_OnWrongPassword() {
            User user = makeUser(1L, "login@test.com");
            user.setPasswordHash("hashed_password");

            when(userRepository.findByEmail("login@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(
                new LoginRequest("login@test.com", "wrong_password")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("throws ValidationException when email not found")
        void throwsValidationException_WhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(
                new LoginRequest("unknown@test.com", "any_password")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("throws ValidationException for disabled account")
        void throwsValidationException_ForDisabledAccount() {
            User user = makeUser(1L, "disabled@test.com");
            user.setPasswordHash("hashed");
            user.setEnabled(false);

            when(userRepository.findByEmail("disabled@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(
                new LoginRequest("disabled@test.com", "password")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Account is disabled");
        }
    }

    // ─── Refresh ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("rotates refresh token and returns new token pair")
        void rotatesRefreshToken_ReturnsNewPair() {
            User user = makeUser(1L, "refresh@test.com");
            user.setRole(Role.PATIENT);

            AuthToken oldToken = new AuthToken(
                user, "valid-refresh-token", "web", null,
                Instant.now().plus(7, ChronoUnit.DAYS)
            );
            oldToken.setId(10L);

            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
            when(authTokenRepository.findByRefreshTokenAndRevokedFalse("valid-refresh-token"))
                .thenReturn(Optional.of(oldToken));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtTokenProvider.generateAccessToken(1L, "refresh@test.com", "PATIENT"))
                .thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken(1L))
                .thenReturn("new-refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);

            AuthResponse response = authService.refresh(
                new RefreshRequest("valid-refresh-token"));

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");

            // Old token should be revoked
            assertThat(oldToken.isRevoked()).isTrue();
            verify(authTokenRepository).save(oldToken);
        }

        @Test
        @DisplayName("throws ValidationException for invalid token")
        void throwsValidationException_ForInvalidToken() {
            when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("throws ValidationException for revoked token")
        void throwsValidationException_ForRevokedToken() {
            when(jwtTokenProvider.validateToken("revoked-token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("revoked-token")).thenReturn(1L);
            when(authTokenRepository.findByRefreshTokenAndRevokedFalse("revoked-token"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("revoked-token")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Refresh token not found");
        }

        @Test
        @DisplayName("throws ValidationException for expired token")
        void throwsValidationException_ForExpiredToken() {
            User user = makeUser(1L, "expired@test.com");
            AuthToken expiredToken = new AuthToken(
                user, "expired-token", "web", null,
                Instant.now().minus(1, ChronoUnit.HOURS)
            );

            when(jwtTokenProvider.validateToken("expired-token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("expired-token")).thenReturn(1L);
            when(authTokenRepository.findByRefreshTokenAndRevokedFalse("expired-token"))
                .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Refresh token has expired");
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private User makeUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hashed");
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setCredentialsExpired(false);
        user.setMfaEnabled(false);
        return user;
    }
}
