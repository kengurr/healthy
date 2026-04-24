package com.zdravdom.auth.adapters.inbound.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Token refresh request validated against OpenAPI contract.
 */
public record RefreshRequest(

    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
