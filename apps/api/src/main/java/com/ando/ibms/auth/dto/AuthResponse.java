package com.ando.ibms.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
