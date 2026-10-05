package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Ticket;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.SeatOverbookedException;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TicketRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SeatOverbookingConcurrencyTest {

    @Autowired
    private TransitService transitService;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long tripId;
    private final List<Long> commuterIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cleanUp();

        Route route = routeRepository.saveAndFlush(new Route("Test Origin", "Test Destination", 12.0, TransitMode.BUS));

        // Create a single-seat trip strictly guarded by @Version
        Trip trip = new Trip(
                route,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                1, // Only 1 seat available!
                new BigDecimal("50.00")
        );
        trip = tripRepository.saveAndFlush(trip);
        this.tripId = trip.getId();

        // Create 10 distinct commuters, each funded with ₹500 wallet balance
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            String email = "overbook_commuter_" + idx + "@mobility.com";
            User commuter = userRepository.findByEmail(email).orElseGet(() -> {
                User u = new User("Commuter " + idx, email, passwordEncoder.encode("pass123"), "980000000" + idx, Role.ROLE_USER);
                Wallet w = new Wallet(u, new BigDecimal("500.00"));
                u.setWallet(w);
                return userRepository.saveAndFlush(u);
            });
            commuterIds.add(commuter.getId());
        }
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        if (tripId != null) {
            List<Booking> bookings = bookingRepository.findByTripId(tripId);
            for (Booking b : bookings) {
                List<Ticket> tickets = ticketRepository.findByBookingId(b.getId());
                ticketRepository.deleteAll(tickets);
                bookingRepository.delete(b);
            }
            tripRepository.findById(tripId).ifPresent(tripRepository::delete);
        }
    }

    @Test
    @DisplayName("10 concurrent threads attempt booking 1 seat: exactly 1 succeeds, 9 fail with SeatOverbookedException, availableSeats equals 0")
    void verifySeatOverbookingConcurrencyProtection() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger overbookedCount = new AtomicInteger(0);
        List<Throwable> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final Long userId = commuterIds.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for simultaneous firing trigger
                    BookingRequest request = new BookingRequest(tripId, 1, PaymentMethod.WALLET, null);
                    transitService.bookSeatsWithRetry(tripId, userId, request);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    Throwable root = t;
                    while (root.getCause() != null && root.getCause() != root &&
                            !(root instanceof SeatOverbookedException)) {
                        root = root.getCause();
                    }
                    if (root instanceof SeatOverbookedException) {
                        overbookedCount.incrementAndGet();
                    } else {
                        unexpectedExceptions.add(t);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All 10 threads should reach starting line within 5s");
        startLatch.countDown(); // FIRE! All 10 threads compete for the 1 seat simultaneously
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All 10 threads should complete within 10s");
        executor.shutdown();

        // Verification of concurrency guarantees
        assertTrue(unexpectedExceptions.isEmpty(), "No unexpected exceptions should occur: " + unexpectedExceptions);
        assertEquals(1, successCount.get(), "Exactly 1 thread must secure the booking");
        assertEquals(9, overbookedCount.get(), "Exactly 9 threads must fail with SeatOverbookedException");

        // Mathematical invariant: availableSeats must be exactly 0, never negative!
        Trip finalTrip = tripRepository.findById(tripId).orElseThrow();
        assertEquals(0, finalTrip.getAvailableSeats(), "Trip availableSeats must be strictly 0, never negative");

        // Database invariant: exactly 1 confirmed booking and 1 ticket created
        List<Booking> bookings = bookingRepository.findByTripId(tripId);
        assertEquals(1, bookings.size(), "Exactly 1 booking record must exist for this trip");
        assertEquals(BookingStatus.CONFIRMED, bookings.get(0).getStatus());

        List<Ticket> tickets = ticketRepository.findByBookingId(bookings.get(0).getId());
        assertEquals(1, tickets.size(), "Exactly 1 ticket must be generated for the booking");
    }
}
