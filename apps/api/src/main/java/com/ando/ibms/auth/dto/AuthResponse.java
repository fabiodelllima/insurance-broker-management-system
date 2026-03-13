package com.ando.ibms.auth.dto;

/** Outbound DTO carrying the token pair and expiration metadata after authentication. */
public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
