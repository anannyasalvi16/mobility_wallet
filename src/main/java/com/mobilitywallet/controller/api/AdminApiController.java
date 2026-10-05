package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.response.AdminAnalyticsDto;
import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.UserResponse;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations & Analytics", description = "Endpoints restricted strictly to ROLE_ADMIN for executive metrics and management")
public class AdminApiController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AdminApiController(AnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    @Operation(summary = "Admin summary diagnostics", description = "Verifies administrative authorization and system status.")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Admin area accessed", Map.of("status", "AUTHORIZED_ADMIN")));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get platform analytics", description = "Computes real-time KPI metrics, voucher status distribution, daily spend, and route load factors.")
    public ResponseEntity<ApiResponse<AdminAnalyticsDto>> getAnalytics() {
        AdminAnalyticsDto analytics = analyticsService.getAdminAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }

    @GetMapping("/commuters")
    @Operation(summary = "List all commuters", description = "Returns all registered commuters for voucher assignment and management.")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getCommuters() {
        List<UserResponse> commuters = userRepository.findByRole(Role.ROLE_USER).stream()
                .map(UserResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Commuters retrieved successfully", commuters));
    }
}
