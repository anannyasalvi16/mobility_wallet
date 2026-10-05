package com.mobilitywallet.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for generating a batch of identical vouchers.
 */
public class VoucherBulkCreateRequest {

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 100, message = "Cannot generate more than 100 vouchers in a single batch")
    private int count = 10;

    @NotNull(message = "Face value is required")
    @Positive(message = "Face value must be positive")
    private BigDecimal initialValue;

    @NotNull(message = "Expiry date is required")
    @FutureOrPresent(message = "Expiry date cannot be in the past")
    private LocalDate expiryDate;

    public VoucherBulkCreateRequest() {
    }

    public VoucherBulkCreateRequest(int count, BigDecimal initialValue, LocalDate expiryDate) {
        this.count = count;
        this.initialValue = initialValue;
        this.expiryDate = expiryDate;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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
