package com.mobilitywallet.dto.request;

import com.mobilitywallet.entity.TransitMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload to create a transit route")
public class RouteCreateRequest {

    @NotBlank(message = "Origin location is required")
    @Size(max = 100, message = "Origin cannot exceed 100 characters")
    @Schema(description = "Starting point", example = "Campus Main Gate")
    private String origin;

    @NotBlank(message = "Destination location is required")
    @Size(max = 100, message = "Destination cannot exceed 100 characters")
    @Schema(description = "Terminating point", example = "Tech Park Station")
    private String destination;

    @NotNull(message = "Transit mode is required")
    @Schema(description = "Mode of transportation (BUS, METRO)", example = "BUS")
    private TransitMode mode;

    @NotNull(message = "Distance in KM is required")
    @DecimalMin(value = "0.1", message = "Distance must be at least 0.1 km")
    @Schema(description = "Travel distance in kilometers", example = "14.5")
    private Double distanceKm;

    public RouteCreateRequest() {
    }

    public RouteCreateRequest(String origin, String destination, TransitMode mode, Double distanceKm) {
        this.origin = origin;
        this.destination = destination;
        this.mode = mode;
        this.distanceKm = distanceKm;
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

    public TransitMode getMode() {
        return mode;
    }

    public void setMode(TransitMode mode) {
        this.mode = mode;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
