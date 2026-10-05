package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.request.RouteCreateRequest;
import com.mobilitywallet.dto.request.TripCreateRequest;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.dto.response.TicketResponse;
import com.mobilitywallet.dto.response.TripSearchResponse;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Trip;

import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for route discovery, schedule lookups, seat bookings with
 * optimistic locking retry protection, and booking cancellations with automated refunds.
 */
public interface TransitService {

    /**
     * Lists all active transit routes across the network.
     */
    List<Route> getAllActiveRoutes();

    /**
     * Creates a new transit route (Admin).
     */
    Route createRoute(RouteCreateRequest request);

    /**
     * Schedules a new trip for a route (Admin).
     */
    Trip createTrip(TripCreateRequest request);

    /**
     * Searches available trips matching origin, destination, and departure date.
     */
    List<TripSearchResponse> searchTrips(String origin, String destination, LocalDate date);

    /**
     * Fetches trip details by ID.
     */
    TripSearchResponse getTripById(Long id);

    /**
     * Books requested seats using optimistic lock collision detection and retry with backoff.
     */
    BookingSummaryResponse bookSeatsWithRetry(Long tripId, Long userId, BookingRequest request);

    /**
     * Cancels an upcoming booking, replenishes available seats, and issues a 100% wallet refund.
     */
    BookingSummaryResponse cancelBooking(Long bookingId, Long userId);

    /**
     * Retrieves all bookings made by a commuter.
     */
    List<BookingSummaryResponse> getCommuterBookings(Long userId);

    /**
     * Retrieves a booking by ID.
     */
    BookingSummaryResponse getBookingById(Long bookingId);

    /**
     * Retrieves ticket details by ID.
     */
    TicketResponse getTicketById(Long ticketId);

    /**
     * Retrieves Base64 QR code data URI for a ticket.
     */
    String getTicketQrCode(Long ticketId);
}
