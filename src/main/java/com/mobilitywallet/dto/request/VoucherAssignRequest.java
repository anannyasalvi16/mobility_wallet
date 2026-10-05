package com.mobilitywallet.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for assigning a voucher to a specific commuter user.
 */
public class VoucherAssignRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    public VoucherAssignRequest() {
    }

    public VoucherAssignRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
