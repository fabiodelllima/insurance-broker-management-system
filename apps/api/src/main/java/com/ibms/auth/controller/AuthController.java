package com.ibms.auth.controller;

import com.ibms.auth.dto.AuthResponse;
import com.ibms.auth.dto.LoginRequest;
import com.ibms.auth.dto.RefreshRequest;
import com.ibms.auth.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing authentication endpoints (login, token refresh, and logout). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /** Creates the controller with the given authentication service. */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Authenticates a user and returns an access/refresh token pair. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Issues a new access token from a valid refresh token. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /** Invalidates the refresh token and returns 204 No Content. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
