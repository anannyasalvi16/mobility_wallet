package com.mobilitywallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Taxi ride entity capturing fare redemptions settled via mobility vouchers.
 */
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false, unique = true)
    private Voucher voucher;

    @Column(nullable = false, length = 100)
    private String driverName;

    @Column(nullable = false, length = 32)
    private String vehicleNumber;

    @Column(nullable = false, length = 150)
    private String pickupLocation;

    @Column(nullable = false, length = 150)
    private String dropLocation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false, updatable = false)
    private LocalDateTime rideTime;

    public Ride() {
    }

    public Ride(User user, Voucher voucher, String driverName, String vehicleNumber, String pickupLocation, String dropLocation, BigDecimal fare) {
        this.user = user;
        this.voucher = voucher;
        this.driverName = driverName;
        this.vehicleNumber = vehicleNumber;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.fare = fare != null ? fare.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    @PrePersist
    protected void onCreate() {
        if (this.rideTime == null) {
            this.rideTime = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
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
        this.fare = fare != null ? fare.setScale(2) : BigDecimal.ZERO.setScale(2);
    }

    public LocalDateTime getRideTime() {
        return rideTime;
    }

    public void setRideTime(LocalDateTime rideTime) {
        this.rideTime = rideTime;
    }
}
