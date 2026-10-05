package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.UserResponse;
import com.mobilitywallet.dto.response.VoucherResponse;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.service.UserService;
import com.mobilitywallet.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')")
@Tag(name = "User API", description = "Endpoints restricted strictly to authenticated commuters (ROLE_USER)")
public class UserApiController {

    private final UserService userService;
    private final VoucherService voucherService;

    public UserApiController(UserService userService, VoucherService voucherService) {
        this.userService = userService;
        this.voucherService = voucherService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get commuter profile", description = "Retrieves profile and wallet details of the authenticated commuter.")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", UserResponse.fromEntity(user)));
    }

    @GetMapping("/vouchers")
    @Operation(summary = "Get commuter vouchers", description = "Retrieves all travel vouchers assigned to the commuter.")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getMyVouchers(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<Voucher> vouchers = voucherService.getVouchersForUser(user.getId());
        List<VoucherResponse> response = vouchers.stream().map(VoucherResponse::fromEntity).toList();
        return ResponseEntity.ok(ApiResponse.success("Vouchers retrieved successfully", response));
    }

    @GetMapping("/active-vouchers")
    @Operation(summary = "Get active commuter vouchers", description = "Retrieves all non-expired, active assigned vouchers ready for travel.")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getMyActiveVouchers(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<Voucher> vouchers = voucherService.getActiveVouchersForUser(user.getId());
        List<VoucherResponse> response = vouchers.stream().map(VoucherResponse::fromEntity).toList();
        return ResponseEntity.ok(ApiResponse.success("Active vouchers retrieved successfully", response));
    }
}
