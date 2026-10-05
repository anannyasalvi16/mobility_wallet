package com.mobilitywallet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payload to schedule a trip on an existing route")
public class TripCreateRequest {

    @NotNull(message = "Route ID is required")
    @Schema(description = "Route identifier", example = "1")
    private Long routeId;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    @Schema(description = "Departure timestamp", example = "2026-10-04T09:00:00")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @Future(message = "Arrival time must be in the future")
    @Schema(description = "Estimated arrival timestamp", example = "2026-10-04T09:45:00")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Total seat capacity is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    @Schema(description = "Total bus/metro carriage seat capacity", example = "40")
    private Integer totalSeats;

    @NotNull(message = "Seat fare is required")
    @DecimalMin(value = "0.01", message = "Fare must be strictly positive")
    @Schema(description = "Ticket price per seat", example = "35.00")
    private BigDecimal fare;

    public TripCreateRequest() {
    }

    public TripCreateRequest(Long routeId, LocalDateTime departureTime, LocalDateTime arrivalTime, Integer totalSeats, BigDecimal fare) {
        this.routeId = routeId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.totalSeats = totalSeats;
        this.fare = fare;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}
