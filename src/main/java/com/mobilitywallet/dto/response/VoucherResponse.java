package com.mobilitywallet.dto.response;

import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data transfer object for voucher information, including status and optional QR data URI.
 */
public class VoucherResponse {

    private Long id;
    private String code;
    private BigDecimal initialValue;
    private VoucherStatus status;
    private LocalDate expiryDate;
    private boolean expired;
    private Long assignedToId;
    private String assignedToName;
    private String assignedToEmail;
    private String createdByEmail;
    private LocalDateTime redeemedAt;
    private LocalDateTime createdAt;
    private String qrCodeDataUri;

    public VoucherResponse() {
    }

    public static VoucherResponse fromEntity(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setInitialValue(voucher.getInitialValue());
        response.setStatus(voucher.getStatus());
        response.setExpiryDate(voucher.getExpiryDate());
        response.setExpired(voucher.isExpired());
        if (voucher.getAssignedTo() != null) {
            response.setAssignedToId(voucher.getAssignedTo().getId());
            response.setAssignedToName(voucher.getAssignedTo().getName());
            response.setAssignedToEmail(voucher.getAssignedTo().getEmail());
        }
        if (voucher.getCreatedBy() != null) {
            response.setCreatedByEmail(voucher.getCreatedBy().getEmail());
        }
        response.setRedeemedAt(voucher.getRedeemedAt());
        response.setCreatedAt(voucher.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = initialValue;
    }

    public VoucherStatus getStatus() {
        return status;
    }

    public void setStatus(VoucherStatus status) {
        this.status = status;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getAssignedToEmail() {
        return assignedToEmail;
    }

    public void setAssignedToEmail(String assignedToEmail) {
        this.assignedToEmail = assignedToEmail;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getQrCodeDataUri() {
        return qrCodeDataUri;
    }

    public void setQrCodeDataUri(String qrCodeDataUri) {
        this.qrCodeDataUri = qrCodeDataUri;
    }
}
