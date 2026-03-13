package com.ando.ibms.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ando.ibms.auth.service.JwtTokenService;
import com.ando.ibms.common.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Unit tests for {@link JwtTokenService}.
 *
 * <p>These tests use a standalone {@link JwtProperties} instance — no Spring context required.
 * Token generation, parsing, validation and expiration are all covered.
 */
class JwtTokenServiceTest {

    private JwtTokenService tokenService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "broker@example.com";
    private static final String ROLE = "BROKER";

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("3f8a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1");
        properties.setExpirationMs(900_000);
        properties.setRefreshExpirationMs(604_800_000);
        tokenService = new JwtTokenService(properties);
    }

    @Test
    void generateAccessToken_shouldProduceValidToken() {
        String token = tokenService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(token).isNotBlank();
        assertThat(tokenService.isTokenValid(token)).isTrue();
    }

    @Test
    void generateRefreshToken_shouldProduceValidToken() {
        String token = tokenService.generateRefreshToken(USER_ID, EMAIL, ROLE);

        assertThat(token).isNotBlank();
        assertThat(tokenService.isTokenValid(token)).isTrue();
    }

    @Test
    void parseToken_shouldReturnCorrectClaims() {
        String token = tokenService.generateAccessToken(USER_ID, EMAIL, ROLE);
        Claims claims = tokenService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.get("email", String.class)).isEqualTo(EMAIL);
        assertThat(claims.get("role", String.class)).isEqualTo(ROLE);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void extractUserId_shouldReturnSubjectAsUuid() {
        String token = tokenService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(tokenService.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    void extractEmail_shouldReturnEmailClaim() {
        String token = tokenService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(tokenService.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    void extractRole_shouldReturnRoleClaim() {
        String token = tokenService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(tokenService.extractRole(token)).isEqualTo(ROLE);
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken() {
        String token = tokenService.generateAccessToken(USER_ID, EMAIL, ROLE);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(tokenService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForTokenSignedWithDifferentKey() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        otherProperties.setExpirationMs(900_000);
        otherProperties.setRefreshExpirationMs(604_800_000);
        JwtTokenService otherService = new JwtTokenService(otherProperties);

        String token = otherService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(tokenService.isTokenValid(token)).isFalse();
    }

    @Test
    void parseToken_shouldThrowForExpiredToken() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("3f8a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1");
        shortLived.setExpirationMs(0);
        shortLived.setRefreshExpirationMs(0);
        JwtTokenService expiredService = new JwtTokenService(shortLived);

        String token = expiredService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThatThrownBy(() -> tokenService.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("3f8a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1");
        shortLived.setExpirationMs(0);
        shortLived.setRefreshExpirationMs(0);
        JwtTokenService expiredService = new JwtTokenService(shortLived);

        String token = expiredService.generateAccessToken(USER_ID, EMAIL, ROLE);

        assertThat(tokenService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForGarbageInput() {
        assertThat(tokenService.isTokenValid("not.a.jwt")).isFalse();
        assertThat(tokenService.isTokenValid("")).isFalse();
        assertThat(tokenService.isTokenValid(null)).isFalse();
    }
}
