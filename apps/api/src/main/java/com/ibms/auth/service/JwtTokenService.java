package com.ibms.auth.service;

import com.ibms.common.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

/** Service responsible for creating, parsing and validating HMAC-signed JWTs. */
@Service
public final class JwtTokenService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    /** Initializes the signing key and TTL values from externalized properties. */
    public JwtTokenService(JwtProperties properties) {
        this.signingKey =
                Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.getExpirationMs();
        this.refreshExpirationMs = properties.getRefreshExpirationMs();
    }

    /** Generates a short-lived access token for the given user. */
    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, expirationMs);
    }

    /** Generates a long-lived refresh token for the given user. */
    public String generateRefreshToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, refreshExpirationMs);
    }

    /** Parses and verifies a signed JWT, returning its claims. */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    /** Returns {@code true} if the token has a valid signature and is not expired. */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Extracts the user ID (subject) from a valid token. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    /** Extracts the email claim from a valid token. */
    public String extractEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    /** Extracts the role claim from a valid token. */
    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    private String buildToken(UUID userId, String email, String role, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }
}
