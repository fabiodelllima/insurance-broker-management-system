package com.ando.ibms.auth.service;

import com.ando.ibms.auth.dto.AuthResponse;
import com.ando.ibms.auth.dto.LoginRequest;
import com.ando.ibms.auth.dto.RefreshRequest;
import com.ando.ibms.auth.model.User;
import com.ando.ibms.auth.repository.UserRepository;
import com.ando.ibms.common.config.JwtProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthenticationException("User not found") {});

        String accessToken = jwtTokenService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name()
        );
        String refreshToken = jwtTokenService.generateRefreshToken(
            user.getId(), user.getEmail(), user.getRole().name()
        );

        return new AuthResponse(accessToken, refreshToken, jwtProperties.getExpirationMs());
    }

    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = jwtTokenService.extractEmail(token);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtTokenService.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name()
        );

        return new AuthResponse(accessToken, token, jwtProperties.getExpirationMs());
    }
}
