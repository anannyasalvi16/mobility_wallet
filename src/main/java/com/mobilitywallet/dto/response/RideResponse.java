package com.mobilitywallet.dto.response;

import com.mobilitywallet.entity.Ride;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Response containing details of a settled taxi ride")
public class RideResponse {

    @Schema(description = "Unique identifier of the ride record", example = "1")
    private Long id;

    @Schema(description = "ID of the commuter who redeemed the voucher", example = "2")
    private Long userId;

    @Schema(description = "Full name of the commuter", example = "John Doe")
    private String userName;

    @Schema(description = "Email address of the commuter", example = "user@mobility.com")
    private String userEmail;

    @Schema(description = "Voucher code redeemed for this trip", example = "VCH-2026-AB12CD")
    private String voucherCode;

    @Schema(description = "Name of the taxi driver", example = "Rajesh Sharma")
    private String driverName;

    @Schema(description = "Vehicle registration number", example = "MH-01-AB-1234")
    private String vehicleNumber;

    @Schema(description = "Trip starting point", example = "Mumbai Airport Terminal 2")
    private String pickupLocation;

    @Schema(description = "Trip destination", example = "Bandra Kurla Complex (BKC)")
    private String dropLocation;

    @Schema(description = "Fare settled against the voucher", example = "450.00")
    private BigDecimal fare;

    @Schema(description = "Timestamp of redemption and ride settlement", example = "2026-10-03T18:30:00")
    private LocalDateTime rideTime;

    public RideResponse() {
    }

    public RideResponse(Long id, Long userId, String userName, String userEmail, String voucherCode,
                        String driverName, String vehicleNumber, String pickupLocation,
                        String dropLocation, BigDecimal fare, LocalDateTime rideTime) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.voucherCode = voucherCode;
        this.driverName = driverName;
        this.vehicleNumber = vehicleNumber;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.fare = fare;
        this.rideTime = rideTime;
    }

    public static RideResponse fromEntity(Ride ride) {
        if (ride == null) {
            return null;
        }
        return new RideResponse(
                ride.getId(),
                ride.getUser() != null ? ride.getUser().getId() : null,
                ride.getUser() != null ? ride.getUser().getName() : null,
                ride.getUser() != null ? ride.getUser().getEmail() : null,
                ride.getVoucher() != null ? ride.getVoucher().getCode() : null,
                ride.getDriverName(),
                ride.getVehicleNumber(),
                ride.getPickupLocation(),
                ride.getDropLocation(),
                ride.getFare(),
                ride.getRideTime()
        );
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public LocalDateTime getRideTime() {
        return rideTime;
    }

    public void setRideTime(LocalDateTime rideTime) {
        this.rideTime = rideTime;
    }
}
