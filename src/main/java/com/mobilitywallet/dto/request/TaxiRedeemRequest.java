package com.mobilitywallet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload to redeem a mobility voucher for a taxi ride")
public class TaxiRedeemRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(max = 64, message = "Voucher code cannot exceed 64 characters")
    @Schema(description = "Unique alphanumeric voucher code", example = "VCH-2026-AB12CD")
    private String voucherCode;

    @NotBlank(message = "Driver name is required")
    @Size(max = 100, message = "Driver name cannot exceed 100 characters")
    @Schema(description = "Name of the taxi driver", example = "Rajesh Sharma")
    private String driverName;

    @NotBlank(message = "Vehicle number is required")
    @Size(max = 32, message = "Vehicle number cannot exceed 32 characters")
    @Schema(description = "License plate / registration number of the taxi", example = "MH-01-AB-1234")
    private String vehicleNumber;

    @NotBlank(message = "Pickup location is required")
    @Size(max = 150, message = "Pickup location cannot exceed 150 characters")
    @Schema(description = "Trip starting point", example = "Mumbai Airport Terminal 2")
    private String pickupLocation;

    @NotBlank(message = "Drop location is required")
    @Size(max = 150, message = "Drop location cannot exceed 150 characters")
    @Schema(description = "Trip destination", example = "Bandra Kurla Complex (BKC)")
    private String dropLocation;

    @NotNull(message = "Fare amount is required")
    @DecimalMin(value = "0.01", message = "Fare must be strictly positive and at least ₹0.01")
    @Schema(description = "Total meter fare settled against the voucher", example = "450.00")
    private BigDecimal fare;

    public TaxiRedeemRequest() {
    }

    public TaxiRedeemRequest(String voucherCode, String driverName, String vehicleNumber, String pickupLocation, String dropLocation, BigDecimal fare) {
        this.voucherCode = voucherCode;
        this.driverName = driverName;
        this.vehicleNumber = vehicleNumber;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.fare = fare;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropLocation() {
        return dropLocation;
    }

    public void setDropLocation(String dropLocation) {
        this.dropLocation = dropLocation;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}
