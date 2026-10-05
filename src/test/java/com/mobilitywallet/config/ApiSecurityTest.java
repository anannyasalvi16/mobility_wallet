package com.mobilitywallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilitywallet.dto.request.RegistrationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("/api/** returns 401 JSON without credentials")
    void apiWithoutCredentialsReturns401Json() throws Exception {
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("/api/** returns 200 with valid Basic auth")
    void apiWithValidBasicAuthReturns200() throws Exception {
        mockMvc.perform(get("/api/admin/summary")
                .with(httpBasic("admin@mobility.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED_ADMIN"));
    }

    @Test
    @DisplayName("/api/** returns 403 JSON for the wrong role")
    void apiWithWrongRoleReturns403Json() throws Exception {
        // User attempts to access admin endpoint -> 403 Forbidden JSON
        mockMvc.perform(get("/api/admin/summary")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("No CSRF token required for /api/** POST requests")
    void apiDoesNotRequireCsrf() throws Exception {
        RegistrationDto dto = new RegistrationDto("API Commuter", "apicommute@test.com", "securePassword123", "9900112233");

        // Notice: NO csrf() token supplied in post request, must succeed with 201 Created
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("apicommute@test.com"))
                .andExpect(jsonPath("$.data.walletBalance").value(0.00));
    }
}
