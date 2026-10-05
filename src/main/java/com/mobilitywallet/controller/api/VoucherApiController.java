package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.request.VoucherAssignRequest;
import com.mobilitywallet.dto.request.VoucherBulkCreateRequest;
import com.mobilitywallet.dto.request.VoucherCreateRequest;
import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.VoucherResponse;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.service.UserService;
import com.mobilitywallet.service.VoucherService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@Tag(name = "Vouchers", description = "Voucher lifecycle management, issuance, assignment, and QR code generation")
public class VoucherApiController {

    private final VoucherService voucherService;
    private final UserService userService;

    public VoucherApiController(VoucherService voucherService, UserService userService) {
        this.voucherService = voucherService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create single voucher", description = "Admin creates a single travel allowance voucher.")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody VoucherCreateRequest request,
            Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        Voucher voucher = voucherService.createVoucher(request, admin);
        return new ResponseEntity<>(
                ApiResponse.success("Voucher created successfully", VoucherResponse.fromEntity(voucher)),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create batch vouchers", description = "Admin generates a batch of vouchers with identical face value.")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> createBulkVouchers(
            @Valid @RequestBody VoucherBulkCreateRequest request,
            Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        List<Voucher> batch = voucherService.createBulkVouchers(request, admin);
        List<VoucherResponse> responses = batch.stream().map(VoucherResponse::fromEntity).toList();
        return new ResponseEntity<>(
                ApiResponse.success(String.format("Generated %d vouchers successfully", batch.size()), responses),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign voucher to user", description = "Admin assigns a CREATED voucher to a registered commuter.")
    public ResponseEntity<ApiResponse<VoucherResponse>> assignVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherAssignRequest request) {
        Voucher assigned = voucherService.assignVoucher(id, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Voucher assigned successfully", VoucherResponse.fromEntity(assigned)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List assigned vouchers", description = "Commuter retrieves all vouchers assigned to their account.")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getMyVouchers(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<Voucher> vouchers = voucherService.getVouchersForUser(user.getId());
        List<VoucherResponse> responses = vouchers.stream().map(VoucherResponse::fromEntity).toList();
        return ResponseEntity.ok(ApiResponse.success("Vouchers retrieved successfully", responses));
    }

    @GetMapping("/{code}/qr")
    @Operation(summary = "Fetch voucher QR code", description = "Generates a verifiable Base64 data URI QR code for a voucher.")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVoucherQrCode(@PathVariable String code) {
        String qrDataUri = voucherService.getVoucherQrCode(code);
        return ResponseEntity.ok(ApiResponse.success("QR code generated", Map.of(
                "code", code,
                "qrDataUri", qrDataUri
        )));
    }
}
