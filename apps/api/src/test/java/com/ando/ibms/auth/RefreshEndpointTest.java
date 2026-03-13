package com.ando.ibms.auth;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ando.ibms.TestcontainersConfig;
import com.ando.ibms.auth.service.JwtTokenService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@code POST /api/v1/auth/refresh}.
 *
 * <p>Obtains a valid refresh token via the login endpoint, then exercises the refresh flow for both
 * happy path and error scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class RefreshEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;

    private String obtainRefreshToken() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                    {"email": "admin@ibms.test", "password": "password123"}
                    """))
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        // Extract refreshToken from JSON manually to avoid Jackson dependency in test
        int start = body.indexOf("\"refreshToken\":\"") + 16;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    @Test
    void refresh_withValidToken_shouldReturnNewAccessToken() throws Exception {
        String refreshToken = obtainRefreshToken();

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    void refresh_withInvalidToken_shouldReturn400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"invalid.jwt.token\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withExpiredToken_shouldReturn400() throws Exception {
        com.ando.ibms.common.config.JwtProperties expiredProps =
                new com.ando.ibms.common.config.JwtProperties();
        expiredProps.setSecret("3f8a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1");
        expiredProps.setExpirationMs(0);
        expiredProps.setRefreshExpirationMs(0);
        JwtTokenService expiredService = new JwtTokenService(expiredProps);

        String expiredToken =
                expiredService.generateRefreshToken(
                        java.util.UUID.randomUUID(), "admin@ibms.test", "ADMIN");

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"" + expiredToken + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withBlankToken_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void refresh_withMissingField_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
