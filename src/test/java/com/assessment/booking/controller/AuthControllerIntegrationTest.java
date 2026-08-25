package com.assessment.booking.controller;

import com.assessment.booking.dto.request.AuthRequest;
import com.assessment.booking.dto.request.RegisterRequest;
import com.assessment.booking.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/login - Should successfully authenticate seeded admin and return JWT token")
    void testLogin_AdminSuccess() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .email("admin@example.com")
                .password("Admin@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.email", is("admin@example.com")))
                .andExpect(jsonPath("$.data.user.role", is("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 401 Unauthorized for incorrect password")
    void testLogin_InvalidPassword() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .email("admin@example.com")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /auth/register - Should successfully register a new user and return token")
    void testRegister_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser" + System.currentTimeMillis() + "@example.com")
                .password("Secret@123")
                .fullName("New Test User")
                .role(Role.ROLE_USER)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.fullName", is("New Test User")));
    }

    @Test
    @DisplayName("POST /auth/register - Should fail with 400 Bad Request if email format is invalid")
    void testRegister_InvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email-address")
                .password("Secret@123")
                .fullName("Invalid Email User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()));
    }
}
