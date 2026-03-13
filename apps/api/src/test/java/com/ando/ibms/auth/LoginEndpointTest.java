package com.ando.ibms.auth;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ando.ibms.TestcontainersConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@code POST /api/v1/auth/login}.
 *
 * <p>Uses a seeded test user (admin@ibms.test / password123) inserted by {@code
 * V100__test_seed_users.sql}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class LoginEndpointTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void login_withValidCredentials_shouldReturnTokens() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"email": "admin@ibms.test", "password": "password123"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"email": "admin@ibms.test", "password": "wrongpassword"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_withNonexistentUser_shouldReturn401() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"email": "ghost@ibms.test", "password": "password123"}
                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBlankEmail_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"email": "", "password": "password123"}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields.email", notNullValue()));
    }

    @Test
    void login_withInvalidEmail_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"email": "not-an-email", "password": "password123"}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields.email", notNullValue()));
    }

    @Test
    void login_withBlankPassword_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"email": "admin@ibms.test", "password": ""}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields.password", notNullValue()));
    }

    @Test
    void login_withEmptyBody_shouldReturn422() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
