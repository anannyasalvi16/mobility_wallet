package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.RideResponse;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.service.RideService;
import com.mobilitywallet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@Tag(name = "Taxi Rides", description = "Taxi ride voucher redemptions, driver verification, and ride ledger")
public class RideApiController {

    private final RideService rideService;
    private final UserService userService;

    public RideApiController(RideService rideService, UserService userService) {
        this.rideService = rideService;
        this.userService = userService;
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem voucher for taxi ride", description = "Redeems an assigned voucher for taxi ride settlement and logs immutable ledger entry.")
    public ResponseEntity<ApiResponse<RideResponse>> redeemVoucher(
            @Valid @RequestBody TaxiRedeemRequest request) {
        RideResponse ride = rideService.redeemVoucher(request);
        return new ResponseEntity<>(
                ApiResponse.success("Taxi ride voucher redeemed successfully", ride),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List commuter taxi rides", description = "Fetches all taxi rides settled with vouchers by the authenticated commuter.")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getMyRides(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<RideResponse> rides = rideService.getRidesByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.success("User taxi rides fetched successfully", rides));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ride details", description = "Fetches detailed information for a specific taxi ride.")
    public ResponseEntity<ApiResponse<RideResponse>> getRideById(@PathVariable Long id) {
        RideResponse ride = rideService.getRideById(id);
        return ResponseEntity.ok(ApiResponse.success("Ride retrieved successfully", ride));
    }

    @GetMapping("/voucher/{voucherCode}")
    @Operation(summary = "Get ride by voucher code", description = "Looks up ride redemption by voucher code.")
    public ResponseEntity<ApiResponse<RideResponse>> getRideByVoucherCode(@PathVariable String voucherCode) {
        RideResponse ride = rideService.getRideByVoucherCode(voucherCode);
        return ResponseEntity.ok(ApiResponse.success("Ride retrieved successfully", ride));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all taxi rides", description = "Admin lists all taxi ride redemptions across the platform.")
    public ResponseEntity<ApiResponse<Page<RideResponse>>> getAllRides(Pageable pageable) {
        Page<RideResponse> rides = rideService.getAllRides(pageable);
        return ResponseEntity.ok(ApiResponse.success("All rides retrieved successfully", rides));
    }
}
