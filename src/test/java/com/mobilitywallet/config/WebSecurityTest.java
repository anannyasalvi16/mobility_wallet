package com.mobilitywallet.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Anonymous access to /admin/** redirects to /login")
    void anonymousAdminRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost/login"));
    }

    @Test
    @DisplayName("Anonymous access to /user/** redirects to /login")
    void anonymousUserRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "regularUser", roles = {"USER"})
    @DisplayName("USER gets 403 on /admin/**")
    void userGetsForbiddenOnAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    @DisplayName("ADMIN gets 403 on /user/**")
    void adminGetsForbiddenOnUserArea() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("ADMIN role gets 200 on /admin/** area")
    void adminGetsOkOnAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@mobility.com", roles = {"USER"})
    @DisplayName("USER role gets 200 on /user/** area")
    void userGetsOkOnUserArea() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Form login succeeds with seeded admin credentials and redirects to /admin/dashboard")
    void adminFormLoginRedirectsToAdminDashboard() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "admin@mobility.com")
                .param("password", "admin123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/dashboard"));
    }

    @Test
    @DisplayName("Form login succeeds with seeded user credentials and redirects to /user/dashboard")
    void userFormLoginRedirectsToUserDashboard() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "user@mobility.com")
                .param("password", "user123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/user/dashboard"));
    }

    @Test
    @DisplayName("Wrong password fails and redirects to /login?error=true")
    void wrongPasswordFails() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "user@mobility.com")
                .param("password", "completelyWrongPassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?error=true"));
    }

    @Test
    @DisplayName("POST without a CSRF token is rejected with 403 on web endpoints")
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "Hacker")
                .param("email", "hacker@test.com")
                .param("password", "password123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Actuator health endpoint is accessible without authentication (permitAll)")
    void actuatorHealthEndpointIsPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }
}

