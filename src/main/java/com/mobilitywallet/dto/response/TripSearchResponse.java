package com.mobilitywallet.dto.response;

import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Details of an available scheduled transit trip")
public class TripSearchResponse {

    @Schema(description = "Trip unique identifier", example = "1")
    private Long id;

    @Schema(description = "Route ID", example = "1")
    private Long routeId;

    @Schema(description = "Starting point", example = "Campus Main Gate")
    private String origin;

    @Schema(description = "Destination", example = "Tech Park Station")
    private String destination;

    @Schema(description = "Transit mode (BUS, METRO)", example = "BUS")
    private TransitMode transitMode;

    @Schema(description = "Route distance in kilometers", example = "14.5")
    private Double distanceKm;

    @Schema(description = "Departure timestamp", example = "2026-10-04T09:00:00")
    private LocalDateTime departureTime;

    @Schema(description = "Arrival timestamp", example = "2026-10-04T09:45:00")
    private LocalDateTime arrivalTime;

    @Schema(description = "Total carriage capacity", example = "40")
    private Integer totalSeats;

    @Schema(description = "Currently available seats", example = "18")
    private Integer availableSeats;

    @Schema(description = "Ticket price per seat", example = "35.00")
    private BigDecimal fare;

    public TripSearchResponse() {
    }

    public TripSearchResponse(Long id, Long routeId, String origin, String destination, TransitMode transitMode,
                              Double distanceKm, LocalDateTime departureTime, LocalDateTime arrivalTime,
                              Integer totalSeats, Integer availableSeats, BigDecimal fare) {
        this.id = id;
        this.routeId = routeId;
        this.origin = origin;
        this.destination = destination;
        this.transitMode = transitMode;
        this.distanceKm = distanceKm;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.fare = fare;
    }

    public static TripSearchResponse fromEntity(Trip trip) {
        if (trip == null) {
            return null;
        }
        return new TripSearchResponse(
                trip.getId(),
                trip.getRoute() != null ? trip.getRoute().getId() : null,
                trip.getRoute() != null ? trip.getRoute().getOrigin() : null,
                trip.getRoute() != null ? trip.getRoute().getDestination() : null,
                trip.getRoute() != null ? trip.getRoute().getMode() : null,
                trip.getRoute() != null ? trip.getRoute().getDistanceKm() : null,
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                trip.getTotalSeats(),
                trip.getAvailableSeats(),
                trip.getFare()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public TransitMode getTransitMode() {
        return transitMode;
    }

    public void setTransitMode(TransitMode transitMode) {
        this.transitMode = transitMode;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
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

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}
