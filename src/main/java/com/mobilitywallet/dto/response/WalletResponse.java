package com.mobilitywallet.dto.response;

import com.mobilitywallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data transfer object for wallet balance inquiry.
 */
public class WalletResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private BigDecimal balance;
    private LocalDateTime updatedAt;

    public WalletResponse() {
    }

    public static WalletResponse fromEntity(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setId(wallet.getId());
        if (wallet.getUser() != null) {
            response.setUserId(wallet.getUser().getId());
            response.setUserEmail(wallet.getUser().getEmail());
        }
        response.setBalance(wallet.getBalance());
        response.setUpdatedAt(wallet.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
