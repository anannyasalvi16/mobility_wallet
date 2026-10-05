package com.mobilitywallet.service.impl;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.request.RouteCreateRequest;
import com.mobilitywallet.dto.request.TripCreateRequest;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.dto.response.TicketResponse;
import com.mobilitywallet.dto.response.TripSearchResponse;
import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Ticket;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.exception.SeatOverbookedException;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TicketRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.service.TransitBookingExecutor;
import com.mobilitywallet.service.TransitService;
import com.mobilitywallet.service.WalletService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TransitServiceImpl implements TransitService {

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TransitBookingExecutor transitBookingExecutor;
    private final WalletService walletService;

    public TransitServiceImpl(RouteRepository routeRepository,
                              TripRepository tripRepository,
                              BookingRepository bookingRepository,
                              TicketRepository ticketRepository,
                              TransitBookingExecutor transitBookingExecutor,
                              WalletService walletService) {
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.transitBookingExecutor = transitBookingExecutor;
        this.walletService = walletService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Route> getAllActiveRoutes() {
        return routeRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Route createRoute(RouteCreateRequest request) {
        Route route = new Route(
                request.getOrigin().trim(),
                request.getDestination().trim(),
                request.getDistanceKm(),
                request.getMode()
        );
        return routeRepository.save(route);
    }

    @Override
    @Transactional
    public Trip createTrip(TripCreateRequest request) {
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", request.getRouteId()));

        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessException("Arrival time must be strictly after departure time.");
        }

        Trip trip = new Trip(
                route,
                request.getDepartureTime(),
                request.getArrivalTime(),
                request.getTotalSeats(),
                request.getFare()
        );
        return tripRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSearchResponse> searchTrips(String origin, String destination, LocalDate date) {
        if (origin == null || destination == null || date == null) {
            throw new BusinessException("Origin, destination, and travel date are required search criteria.");
        }

        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);

        return tripRepository.searchAvailableTrips(origin.trim(), destination.trim(), startTime, endTime)
                .stream()
                .map(TripSearchResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripSearchResponse getTripById(Long id) {
        Trip trip = tripRepository.findByIdWithRoute(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", id));
        return TripSearchResponse.fromEntity(trip);
    }

    @Override
    public BookingSummaryResponse bookSeatsWithRetry(Long tripId, Long userId, BookingRequest request) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return transitBookingExecutor.executeBookingTransaction(tripId, userId, request);
            } catch (OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new SeatOverbookedException("The requested seats were claimed by another commuter. Please try again.");
                }
                try {
                    Thread.sleep(50L * attempt); // Exponential jitter backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("Seat booking process was interrupted.");
                }
            }
        }

        throw new SeatOverbookedException("The requested seats could not be confirmed due to high concurrent demand.");
    }

    @Override
    @Transactional
    public BookingSummaryResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException("You are not authorized to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("This booking has already been cancelled.");
        }

        if (booking.getTrip().getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot cancel a booking for a trip that has already departed.");
        }

        // 1. Mark booking as CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(booking);

        // 2. Replenish available seats on the trip
        Trip trip = booking.getTrip();
        trip.setAvailableSeats(trip.getAvailableSeats() + booking.getSeatCount());
        tripRepository.saveAndFlush(trip);

        // 3. Issue automated 100% wallet refund
        walletService.credit(
                userId,
                booking.getTotalFare(),
                String.format("Refund for cancelled transit booking %s", booking.getBookingReference()),
                null
        );

        return BookingSummaryResponse.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getCommuterBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByBookingDateDesc(userId)
                .stream()
                .map(BookingSummaryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSummaryResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        return BookingSummaryResponse.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        return TicketResponse.fromEntity(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTicketQrCode(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        return ticket.getQrCodeData();
    }
}
