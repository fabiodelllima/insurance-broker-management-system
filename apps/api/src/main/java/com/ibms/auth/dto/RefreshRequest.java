package com.ibms.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Inbound DTO carrying a refresh token for the token renewal endpoint. */
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {}
