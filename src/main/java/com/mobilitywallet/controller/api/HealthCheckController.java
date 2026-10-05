package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check controller to verify API availability and runtime configuration.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "System health check and diagnostic endpoints")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "Check API status", description = "Returns system status and timestamp")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> status = Map.of(
                "status", "UP",
                "service", "Mobility Wallet API",
                "version", "1.0.0-SNAPSHOT"
        );
        return ResponseEntity.ok(ApiResponse.success("Service is healthy and operational", status));
    }
}
