package com.ando.ibms.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ando.ibms.InMemoryRefreshTokenStore;
import com.ando.ibms.TestcontainersConfig;

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
 * Integration tests for {@code POST /api/v1/auth/logout}.
 *
 * <p>Verifies that logout invalidates the refresh token, preventing subsequent refresh attempts
 * from succeeding.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, InMemoryRefreshTokenStore.class})
class LogoutEndpointTest {

    @Autowired private MockMvc mockMvc;

    private String obtainRefreshToken() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                    {"email": "admin@ibms.test", "password": "password123"}
                    """))
                        .andExpect(status().isOk())
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"refreshToken\":\"") + 16;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    @Test
    void logout_shouldReturn204() throws Exception {
        String refreshToken = obtainRefreshToken();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void refresh_afterLogout_shouldFail() throws Exception {
        String refreshToken = obtainRefreshToken();

        // Logout
        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        // Attempt refresh with revoked token
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withInvalidToken_shouldStillReturn204() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"invalid.jwt.token\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_withBlankToken_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\": \"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
