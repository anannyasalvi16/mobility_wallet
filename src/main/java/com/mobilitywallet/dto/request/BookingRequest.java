package com.mobilitywallet.dto.request;

import com.mobilitywallet.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload to book seats on a transit trip")
public class BookingRequest {

    @NotNull(message = "Trip ID is required")
    @Schema(description = "Target scheduled trip ID", example = "1")
    private Long tripId;

    @NotNull(message = "Seat count is required")
    @Min(value = 1, message = "Must book at least 1 seat")
    @Max(value = 10, message = "Cannot book more than 10 seats per transaction")
    @Schema(description = "Number of passenger seats requested", example = "2")
    private Integer seatCount;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment choice: WALLET (digital balance) or VOUCHER (mobility voucher / split payment)", example = "WALLET")
    private PaymentMethod paymentMethod;

    @Size(max = 64, message = "Voucher code cannot exceed 64 characters")
    @Schema(description = "Voucher code (mandatory if paymentMethod is VOUCHER)", example = "VCH-2026-AB12CD")
    private String voucherCode;

    public BookingRequest() {
    }

    public BookingRequest(Long tripId, Integer seatCount, PaymentMethod paymentMethod, String voucherCode) {
        this.tripId = tripId;
        this.seatCount = seatCount;
        this.paymentMethod = paymentMethod;
        this.voucherCode = voucherCode;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
}
