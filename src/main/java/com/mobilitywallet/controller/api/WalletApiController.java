package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.TransactionResponse;
import com.mobilitywallet.dto.response.WalletResponse;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.service.UserService;
import com.mobilitywallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet & Ledger", description = "Commuter digital wallet inquiries and immutable transaction history")
public class WalletApiController {

    private final WalletService walletService;
    private final UserService userService;

    public WalletApiController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get wallet balance", description = "Commuter checks their current available wallet balance.")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Wallet fetched successfully", WalletResponse.fromEntity(wallet)));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get paginated transaction ledger", description = "Retrieves immutable credit and debit ledger entries.")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            Authentication authentication,
            Pageable pageable) {
        User user = userService.findByEmail(authentication.getName());
        Page<Transaction> page = walletService.getTransactionHistory(user.getId(), pageable);
        Page<TransactionResponse> responsePage = page.map(TransactionResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", responsePage));
    }
}
