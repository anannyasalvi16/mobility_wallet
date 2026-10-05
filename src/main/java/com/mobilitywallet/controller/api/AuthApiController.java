package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.request.RegistrationDto;
import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.UserResponse;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, authentication, and session identity endpoints")
public class AuthApiController {

    private final UserService userService;

    public AuthApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user account", description = "Registers a new commuter user and auto-provisions their wallet with ₹0.00 balance.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegistrationDto request) {
        User user = userService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success("User registered successfully", UserResponse.fromEntity(user)),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns profile and wallet details of the currently authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", UserResponse.fromEntity(user)));
    }
}
