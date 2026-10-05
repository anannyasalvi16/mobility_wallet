package com.mobilitywallet.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminAnalyticsDto {

    private long totalVouchersCount;
    private long createdVouchersCount;
    private long assignedVouchersCount;
    private long redeemedVouchersCount;
    private long expiredVouchersCount;

    private BigDecimal totalIssuedAmount = BigDecimal.ZERO;
    private BigDecimal totalRedeemedAmount = BigDecimal.ZERO;
    private BigDecimal totalWalletBalance = BigDecimal.ZERO;

    private long totalBookingsCount;
    private long totalRidesCount;
    private long totalUsersCount;

    private List<String> dailySpendLabels = new ArrayList<>();
    private List<BigDecimal> dailySpendValues = new ArrayList<>();

    private List<String> routeLabels = new ArrayList<>();
    private List<BigDecimal> routeRevenues = new ArrayList<>();
    private List<Double> routeLoadFactors = new ArrayList<>();

    public AdminAnalyticsDto() {
    }

    public long getTotalVouchersCount() {
        return totalVouchersCount;
    }

    public void setTotalVouchersCount(long totalVouchersCount) {
        this.totalVouchersCount = totalVouchersCount;
    }

    public long getCreatedVouchersCount() {
        return createdVouchersCount;
    }

    public void setCreatedVouchersCount(long createdVouchersCount) {
        this.createdVouchersCount = createdVouchersCount;
    }

    public long getAssignedVouchersCount() {
        return assignedVouchersCount;
    }

    public void setAssignedVouchersCount(long assignedVouchersCount) {
        this.assignedVouchersCount = assignedVouchersCount;
    }

    public long getRedeemedVouchersCount() {
        return redeemedVouchersCount;
    }

    public void setRedeemedVouchersCount(long redeemedVouchersCount) {
        this.redeemedVouchersCount = redeemedVouchersCount;
    }

    public long getExpiredVouchersCount() {
        return expiredVouchersCount;
    }

    public void setExpiredVouchersCount(long expiredVouchersCount) {
        this.expiredVouchersCount = expiredVouchersCount;
    }

    public BigDecimal getTotalIssuedAmount() {
        return totalIssuedAmount;
    }

    public void setTotalIssuedAmount(BigDecimal totalIssuedAmount) {
        this.totalIssuedAmount = totalIssuedAmount;
    }

    public BigDecimal getTotalRedeemedAmount() {
        return totalRedeemedAmount;
    }

    public void setTotalRedeemedAmount(BigDecimal totalRedeemedAmount) {
        this.totalRedeemedAmount = totalRedeemedAmount;
    }

    public BigDecimal getTotalWalletBalance() {
        return totalWalletBalance;
    }

    public void setTotalWalletBalance(BigDecimal totalWalletBalance) {
        this.totalWalletBalance = totalWalletBalance;
    }

    public long getTotalBookingsCount() {
        return totalBookingsCount;
    }

    public void setTotalBookingsCount(long totalBookingsCount) {
        this.totalBookingsCount = totalBookingsCount;
    }

    public long getTotalRidesCount() {
        return totalRidesCount;
    }

    public void setTotalRidesCount(long totalRidesCount) {
        this.totalRidesCount = totalRidesCount;
    }

    public long getTotalUsersCount() {
        return totalUsersCount;
    }

    public void setTotalUsersCount(long totalUsersCount) {
        this.totalUsersCount = totalUsersCount;
    }

    public List<String> getDailySpendLabels() {
        return dailySpendLabels;
    }

    public void setDailySpendLabels(List<String> dailySpendLabels) {
        this.dailySpendLabels = dailySpendLabels;
    }

    public List<BigDecimal> getDailySpendValues() {
        return dailySpendValues;
    }

    public void setDailySpendValues(List<BigDecimal> dailySpendValues) {
        this.dailySpendValues = dailySpendValues;
    }

    public List<String> getRouteLabels() {
        return routeLabels;
    }

    public void setRouteLabels(List<String> routeLabels) {
        this.routeLabels = routeLabels;
    }

    public List<BigDecimal> getRouteRevenues() {
        return routeRevenues;
    }

    public void setRouteRevenues(List<BigDecimal> routeRevenues) {
        this.routeRevenues = routeRevenues;
    }

    public List<Double> getRouteLoadFactors() {
        return routeLoadFactors;
    }

    public void setRouteLoadFactors(List<Double> routeLoadFactors) {
        this.routeLoadFactors = routeLoadFactors;
    }
}
