package com.mobilitywallet.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for admin creating a single mobility voucher.
 */
public class VoucherCreateRequest {

    @NotNull(message = "Voucher initial value is required")
    @Positive(message = "Voucher value must be strictly positive")
    private BigDecimal initialValue;

    @NotNull(message = "Expiry date is required")
    @FutureOrPresent(message = "Expiry date cannot be in the past")
    private LocalDate expiryDate;

    public VoucherCreateRequest() {
    }

    public VoucherCreateRequest(BigDecimal initialValue, LocalDate expiryDate) {
        this.initialValue = initialValue;
        this.expiryDate = expiryDate;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = initialValue;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
