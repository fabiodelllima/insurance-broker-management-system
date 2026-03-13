package com.ibms.auth.service;

import com.ibms.auth.dto.AuthResponse;
import com.ibms.auth.dto.LoginRequest;
import com.ibms.auth.dto.RefreshRequest;
import com.ibms.auth.model.User;
import com.ibms.auth.repository.UserRepository;
import com.ibms.common.config.JwtProperties;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/** Orchestrates authentication workflows: credential-based login, token refresh, and logout. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

    /** Creates the service with its required collaborators. */
    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            RefreshTokenStore refreshTokenStore) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenStore = refreshTokenStore;
    }

    /** Authenticates by email/password and returns an access/refresh token pair. */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(() -> new AuthenticationException("User not found") {});

        String accessToken =
                jwtTokenService.generateAccessToken(
                        user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken =
                jwtTokenService.generateRefreshToken(
                        user.getId(), user.getEmail(), user.getRole().name());

        refreshTokenStore.store(refreshToken, user.getId(), jwtProperties.getRefreshExpirationMs());

        return new AuthResponse(accessToken, refreshToken, jwtProperties.getExpirationMs());
    }

    /** Validates a refresh token and issues a new access token. */
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        if (!refreshTokenStore.exists(token)) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        String email = jwtTokenService.extractEmail(token);
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken =
                jwtTokenService.generateAccessToken(
                        user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(accessToken, token, jwtProperties.getExpirationMs());
    }

    /** Invalidates a refresh token, preventing further use. */
    public void logout(RefreshRequest request) {
        String token = request.refreshToken();

        if (jwtTokenService.isTokenValid(token)) {
            refreshTokenStore.delete(token);
        }
    }
}
