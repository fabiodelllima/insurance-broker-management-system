package com.ibms.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibms.InMemoryRefreshTokenStore;
import com.ibms.TestcontainersConfig;
import com.ibms.auth.service.JwtTokenService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * Integration tests for the JWT security filter chain.
 *
 * <p>Verifies that public routes are accessible without authentication, protected routes reject
 * unauthenticated requests, and valid JWTs grant access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, InMemoryRefreshTokenStore.class})
class SecurityChainTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;

    @Test
    void publicRoute_actuatorHealth_shouldReturn200() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedRoute_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/brokers")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withValidToken_shouldNotBeRejectedBySecurityChain() throws Exception {
        String token =
                jwtTokenService.generateAccessToken(
                        UUID.randomUUID(), "admin@example.com", "ADMIN");

        int status =
                mockMvc.perform(get("/api/v1/brokers").header("Authorization", "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void protectedRoute_withInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/brokers").header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withExpiredToken_shouldReturn401() throws Exception {
        com.ibms.common.config.JwtProperties expiredProps =
                new com.ibms.common.config.JwtProperties();
        expiredProps.setSecret("3f8a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1");
        expiredProps.setExpirationMs(0);
        expiredProps.setRefreshExpirationMs(0);
        JwtTokenService expiredService = new JwtTokenService(expiredProps);

        String token =
                expiredService.generateAccessToken(UUID.randomUUID(), "admin@example.com", "ADMIN");

        mockMvc.perform(get("/api/v1/brokers").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authEndpoint_withoutToken_shouldNotBeBlockedBySecurity() throws Exception {
        int status =
                mockMvc.perform(get("/api/v1/auth/nonexistent"))
                        .andReturn()
                        .getResponse()
                        .getStatus();

        assertThat(status).isNotIn(401, 403);
    }
}
