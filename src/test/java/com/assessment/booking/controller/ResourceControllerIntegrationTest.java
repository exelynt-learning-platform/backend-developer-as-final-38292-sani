package com.assessment.booking.controller;

import com.assessment.booking.dto.request.ResourceCreateRequest;
import com.assessment.booking.dto.request.ResourceUpdateRequest;
import com.assessment.booking.entity.Resource;
import com.assessment.booking.repository.ResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    @DisplayName("GET /resources - Should allow authenticated USER to view paginated resources")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testGetAllResources_AsUser() throws Exception {
        mockMvc.perform(get("/resources")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }

    @Test
    @DisplayName("POST /resources - Should allow ADMIN to create a new resource")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testCreateResource_AsAdmin_Success() throws Exception {
        ResourceCreateRequest request = ResourceCreateRequest.builder()
                .name("Test Lab VR Suite " + System.currentTimeMillis())
                .description("VR Testing Room with Meta Quest 3 headsets")
                .type("LAB")
                .capacity(4)
                .location("Building 3, Floor 1")
                .pricePerHour(new BigDecimal("60.00"))
                .active(true)
                .build();

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is(request.getName())))
                .andExpect(jsonPath("$.data.pricePerHour", is(60.00)));
    }

    @Test
    @DisplayName("POST /resources - Should return 403 Forbidden when regular USER attempts to create resource")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testCreateResource_AsUser_Forbidden() throws Exception {
        ResourceCreateRequest request = ResourceCreateRequest.builder()
                .name("Unauthorized Resource")
                .capacity(2)
                .pricePerHour(new BigDecimal("50.00"))
                .build();

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /resources/{id} - Should return 403 Forbidden when regular USER attempts to update resource")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testUpdateResource_AsUser_Forbidden() throws Exception {
        Resource resource = resourceRepository.findAll().get(0);

        ResourceUpdateRequest request = ResourceUpdateRequest.builder()
                .name("Hacked Name")
                .capacity(10)
                .pricePerHour(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(put("/resources/" + resource.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /resources/{id} - Should return 403 Forbidden when regular USER attempts to delete resource")
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void testDeleteResource_AsUser_Forbidden() throws Exception {
        Resource resource = resourceRepository.findAll().get(0);

        mockMvc.perform(delete("/resources/" + resource.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /resources - Should return 401 Unauthorized for unauthenticated request")
    void testGetAllResources_Unauthenticated() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized());
    }
}
