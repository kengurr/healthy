package com.zdravdom.notification.application.dto;

import com.zdravdom.notification.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Register push token request DTO.
 */
public record RegisterPushTokenRequest(
    @NotBlank String token,
    @NotNull Platform platform
) {}