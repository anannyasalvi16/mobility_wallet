package com.mobilitywallet.controller.api;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.request.RouteCreateRequest;
import com.mobilitywallet.dto.request.TripCreateRequest;
import com.mobilitywallet.dto.response.ApiResponse;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.dto.response.TicketResponse;
import com.mobilitywallet.dto.response.TripSearchResponse;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.service.TransitService;
import com.mobilitywallet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transit")
@Tag(name = "Transit & Ticketing", description = "Routes, scheduled trips, seat reservations with optimistic locking, and cancellations")
public class TransitApiController {

    private final TransitService transitService;
    private final UserService userService;

    public TransitApiController(TransitService transitService, UserService userService) {
        this.transitService = transitService;
        this.userService = userService;
    }

    @GetMapping("/routes")
    @Operation(summary = "List active transit routes", description = "Fetches all active routes across the transit network.")
    public ResponseEntity<ApiResponse<List<Route>>> getActiveRoutes() {
        List<Route> routes = transitService.getAllActiveRoutes();
        return ResponseEntity.ok(ApiResponse.success("Active routes fetched", routes));
    }

    @PostMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create route", description = "Admin creates a new transit route.")
    public ResponseEntity<ApiResponse<Route>> createRoute(@Valid @RequestBody RouteCreateRequest request) {
        Route route = transitService.createRoute(request);
        return new ResponseEntity<>(ApiResponse.success("Route created successfully", route), HttpStatus.CREATED);
    }

    @PostMapping("/trips")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Schedule trip", description = "Admin schedules a new transit trip on a route.")
    public ResponseEntity<ApiResponse<TripSearchResponse>> scheduleTrip(@Valid @RequestBody TripCreateRequest request) {
        Trip trip = transitService.createTrip(request);
        return new ResponseEntity<>(ApiResponse.success("Trip scheduled successfully", TripSearchResponse.fromEntity(trip)), HttpStatus.CREATED);
    }

    @GetMapping("/trips/search")
    @Operation(summary = "Search scheduled trips", description = "Finds available trips with open seats for origin, destination, and departure date.")
    public ResponseEntity<ApiResponse<List<TripSearchResponse>>> searchTrips(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TripSearchResponse> results = transitService.searchTrips(origin, destination, date);
        return ResponseEntity.ok(ApiResponse.success("Matching trips found", results));
    }

    @GetMapping("/trips/{id}")
    @Operation(summary = "Get trip details", description = "Retrieves schedule, route details, and remaining seat count.")
    public ResponseEntity<ApiResponse<TripSearchResponse>> getTripById(@PathVariable Long id) {
        TripSearchResponse trip = transitService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.success("Trip details fetched", trip));
    }

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Book seats on trip", description = "Reserves seats with optimistic concurrency retry and settles payment via digital wallet or voucher.")
    public ResponseEntity<ApiResponse<BookingSummaryResponse>> bookSeats(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        BookingSummaryResponse booking = transitService.bookSeatsWithRetry(request.getTripId(), user.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Seats booked successfully", booking), HttpStatus.CREATED);
    }

    @PostMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel booking", description = "Cancels an upcoming confirmed booking, replenishes available seats, and issues a 100% wallet refund.")
    public ResponseEntity<ApiResponse<BookingSummaryResponse>> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        BookingSummaryResponse cancelled = transitService.cancelBooking(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled and 100% fare refunded to wallet", cancelled));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List commuter bookings", description = "Fetches all bookings and ticket history for the authenticated commuter.")
    public ResponseEntity<ApiResponse<List<BookingSummaryResponse>>> getMyBookings(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<BookingSummaryResponse> bookings = transitService.getCommuterBookings(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookings));
    }

    @GetMapping("/bookings/{id}")
    @Operation(summary = "Get booking details", description = "Fetches a booking summary and associated seat tickets.")
    public ResponseEntity<ApiResponse<BookingSummaryResponse>> getBookingById(@PathVariable Long id) {
        BookingSummaryResponse booking = transitService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success("Booking details fetched", booking));
    }

    @GetMapping("/tickets/{id}")
    @Operation(summary = "Get ticket details", description = "Fetches individual ticket verification information.")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = transitService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket details fetched", ticket));
    }

    @GetMapping("/tickets/{id}/qr")
    @Operation(summary = "Get ticket QR code", description = "Fetches Base64 QR code data URI for conductor scanning.")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTicketQrCode(@PathVariable Long id) {
        String qrDataUri = transitService.getTicketQrCode(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket QR code generated", Map.of(
                "ticketId", String.valueOf(id),
                "qrDataUri", qrDataUri
        )));
    }
}
