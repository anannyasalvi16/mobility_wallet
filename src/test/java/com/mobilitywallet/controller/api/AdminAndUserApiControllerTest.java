package com.mobilitywallet.controller.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAndUserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to /api/admin/analytics returns 401 Unauthorized")
    void unauthenticatedAdminAnalyticsFails() throws Exception {
        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@mobility.com", roles = {"USER"})
    @DisplayName("Commuter request to /api/admin/analytics returns 403 Forbidden")
    void commuterForbiddenOnAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin can fetch analytics from /api/admin/analytics")
    void adminCanFetchAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVouchersCount").isNumber())
                .andExpect(jsonPath("$.data.dailySpendLabels").isArray());
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin can fetch commuters list from /api/admin/commuters")
    void adminCanFetchCommuters() throws Exception {
        mockMvc.perform(get("/api/admin/commuters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "user@mobility.com", roles = {"USER"})
    @DisplayName("Commuter can fetch profile from /api/user/profile")
    void commuterCanFetchProfile() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@mobility.com"));
    }

    @Test
    @WithMockUser(username = "user@mobility.com", roles = {"USER"})
    @DisplayName("Commuter can fetch assigned vouchers from /api/user/vouchers")
    void commuterCanFetchVouchers() throws Exception {
        mockMvc.perform(get("/api/user/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin receives 403 Forbidden on /api/user/profile")
    void adminForbiddenOnUserProfile() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OpenAPI specification is accessible at /v3/api-docs")
    void openApiDocsAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.0.1"))
                .andExpect(jsonPath("$.info.title").value("Mobility Wallet API"));
    }
}
