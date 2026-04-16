package com.zdravdom.auth.adapters.inbound.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for JWT authentication and RBAC.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Patient-only endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/me").hasRole("PATIENT")
                .requestMatchers(HttpMethod.PUT, "/api/v1/patients/me").hasRole("PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/me/addresses").hasRole("PATIENT")
                .requestMatchers(HttpMethod.POST, "/api/v1/patients/me/addresses").hasRole("PATIENT")
                .requestMatchers(HttpMethod.POST, "/api/v1/patients/me/documents").hasRole("PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/me/gdpr/export").hasRole("PATIENT")

                // Provider-only endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/providers/me").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/providers/me/availability").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.POST, "/api/v1/providers/me/documents").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.GET, "/api/v1/providers/inbox").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/booking/*/accept").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/booking/*/reject").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/visits/*/start").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/visits/*/complete").hasRole("PROVIDER")
                .requestMatchers(HttpMethod.POST, "/api/v1/visits/*/escalate").hasRole("PROVIDER")

                // Booking endpoints (patients)
                .requestMatchers(HttpMethod.POST, "/api/v1/booking").hasAnyRole("PATIENT", "PROVIDER")
                .requestMatchers(HttpMethod.GET, "/api/v1/booking/**").hasAnyRole("PATIENT", "PROVIDER", "OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/booking/*/cancel").hasAnyRole("PATIENT", "PROVIDER", "OPERATOR", "ADMIN")

                // Visit endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/visits/*/pdf").hasAnyRole("PATIENT", "PROVIDER", "OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/visits/*/send-to-patient").hasAnyRole("PROVIDER", "OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/visits/*/rating").hasRole("PATIENT")

                // Service catalog - all authenticated users
                .requestMatchers(HttpMethod.GET, "/api/v1/services/**").authenticated()

                // Provider matching - all authenticated users
                .requestMatchers(HttpMethod.GET, "/api/v1/providers").authenticated()

                // Payment endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/payments/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").authenticated()

                // Notification endpoints - all authenticated
                .requestMatchers("/api/v1/notifications/**").authenticated()

                // Admin only endpoints
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:8081",
            "https://zdravdom.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}