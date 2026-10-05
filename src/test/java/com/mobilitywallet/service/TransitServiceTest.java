package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.request.RouteCreateRequest;
import com.mobilitywallet.dto.request.TripCreateRequest;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.dto.response.TripSearchResponse;
import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Ticket;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InsufficientBalanceException;
import com.mobilitywallet.exception.SeatOverbookedException;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TicketRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TransitServiceTest {

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
    private VoucherRepository voucherRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User commuter;
    private Route testRoute;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin-transit-test@mobility.com").orElseGet(() -> {
            User a = new User("Transit Admin", "admin-transit-test@mobility.com", passwordEncoder.encode("admin123"), "9222200000", Role.ROLE_ADMIN);
            Wallet w = new Wallet(a, BigDecimal.ZERO);
            a.setWallet(w);
            return userRepository.saveAndFlush(a);
        });

        commuter = userRepository.findByEmail("user-transit-test@mobility.com").orElseGet(() -> {
            User c = new User("Transit Commuter", "user-transit-test@mobility.com", passwordEncoder.encode("user123"), "9222211111", Role.ROLE_USER);
            Wallet w = new Wallet(c, new BigDecimal("500.00"));
            c.setWallet(w);
            return userRepository.saveAndFlush(c);
        });

        // Ensure clean wallet balance
        Wallet w = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        w.setBalance(new BigDecimal("500.00"));
        walletRepository.saveAndFlush(w);

        testRoute = routeRepository.saveAndFlush(new Route("Campus Gate A", "Downtown Hub", 10.0, TransitMode.BUS));

        testTrip = tripRepository.saveAndFlush(new Trip(
                testRoute,
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(45),
                30,
                new BigDecimal("40.00")
        ));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (testTrip != null && testTrip.getId() != null) {
            List<Booking> bookings = bookingRepository.findByTripId(testTrip.getId());
            for (Booking b : bookings) {
                ticketRepository.deleteAll(ticketRepository.findByBookingId(b.getId()));
                bookingRepository.delete(b);
            }
            tripRepository.findById(testTrip.getId()).ifPresent(tripRepository::delete);
        }
        if (testRoute != null && testRoute.getId() != null) {
            routeRepository.findById(testRoute.getId()).ifPresent(routeRepository::delete);
        }
        transactionRepository.findByVoucherCode("VCH-TRANSIT-FULL-1").forEach(transactionRepository::delete);
        voucherRepository.findByCode("VCH-TRANSIT-FULL-1").ifPresent(voucherRepository::delete);

        transactionRepository.findByVoucherCode("VCH-TRANSIT-SPLIT-1").forEach(transactionRepository::delete);
        voucherRepository.findByCode("VCH-TRANSIT-SPLIT-1").ifPresent(voucherRepository::delete);
    }

    @Test
    @DisplayName("Search trips by origin, destination, and departure date returns available schedules")
    void testSearchTrips() {
        List<TripSearchResponse> results = transitService.searchTrips("Campus Gate A", "Downtown Hub", LocalDate.now().plusDays(2));
        assertFalse(results.isEmpty());
        assertEquals("Campus Gate A", results.get(0).getOrigin());
        assertEquals("Downtown Hub", results.get(0).getDestination());
        assertEquals(30, results.get(0).getAvailableSeats());
    }

    @Test
    @DisplayName("Admin can create route and schedule new trip")
    void testCreateRouteAndTrip() {
        Route route = transitService.createRoute(new RouteCreateRequest("Station X", "Airport Z", TransitMode.METRO, 25.0));
        assertNotNull(route.getId());
        assertEquals("Station X", route.getOrigin());

        Trip trip = transitService.createTrip(new TripCreateRequest(
                route.getId(),
                LocalDateTime.now().plusDays(3).withHour(8).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(8).withMinute(30),
                50,
                new BigDecimal("60.00")
        ));
        assertNotNull(trip.getId());
        assertEquals(50, trip.getTotalSeats());
        assertEquals(50, trip.getAvailableSeats());
    }

    @Test
    @DisplayName("Book seats with WALLET payment deducts balance, decrements seats, and issues QR tickets")
    void testBookSeatsWithWallet() {
        BookingRequest request = new BookingRequest(testTrip.getId(), 2, PaymentMethod.WALLET, null);
        BookingSummaryResponse summary = transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request);

        assertNotNull(summary);
        assertEquals(2, summary.getSeatCount());
        assertEquals(0, new BigDecimal("80.00").compareTo(summary.getTotalFare()));
        assertEquals(BookingStatus.CONFIRMED, summary.getStatus());
        assertEquals(2, summary.getTickets().size());
        assertTrue(summary.getTickets().get(0).getQrCodeData().startsWith("data:image/png;base64,"));

        // Verify remaining available seats on trip
        Trip updatedTrip = tripRepository.findById(testTrip.getId()).orElseThrow();
        assertEquals(28, updatedTrip.getAvailableSeats());

        // Verify wallet deducted by 80.00 (500.00 - 80.00 = 420.00)
        Wallet updatedWallet = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("420.00").compareTo(updatedWallet.getBalance()));
    }

    @Test
    @DisplayName("Book seats with full VOUCHER coverage marks voucher REDEEMED and preserves wallet balance")
    void testBookSeatsWithVoucher_FullCoverage() {
        Voucher voucher = new Voucher("VCH-TRANSIT-FULL-1", new BigDecimal("100.00"), LocalDate.now().plusMonths(1), admin);
        voucher.setAssignedTo(commuter);
        voucher.setStatus(VoucherStatus.ASSIGNED);
        voucher = voucherRepository.saveAndFlush(voucher);

        BookingRequest request = new BookingRequest(testTrip.getId(), 2, PaymentMethod.VOUCHER, voucher.getCode()); // fare = 80.00
        BookingSummaryResponse summary = transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request);

        assertNotNull(summary);
        assertEquals(BookingStatus.CONFIRMED, summary.getStatus());
        assertEquals(0, new BigDecimal("80.00").compareTo(summary.getVoucherAmountUsed()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getWalletAmountUsed()));

        // Voucher must be REDEEMED
        Voucher updatedVoucher = voucherRepository.findByCode(voucher.getCode()).orElseThrow();
        assertEquals(VoucherStatus.REDEEMED, updatedVoucher.getStatus());

        // Wallet balance untouched at 500.00
        Wallet updatedWallet = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(updatedWallet.getBalance()));
    }

    @Test
    @DisplayName("Book seats with split payment: voucher covers face value, remaining balance deducted from wallet")
    void testBookSeatsWithVoucher_SplitPayment() {
        Voucher voucher = new Voucher("VCH-TRANSIT-SPLIT-1", new BigDecimal("50.00"), LocalDate.now().plusMonths(1), admin);
        voucher.setAssignedTo(commuter);
        voucher.setStatus(VoucherStatus.ASSIGNED);
        voucher = voucherRepository.saveAndFlush(voucher);

        BookingRequest request = new BookingRequest(testTrip.getId(), 2, PaymentMethod.VOUCHER, voucher.getCode()); // fare = 80.00
        BookingSummaryResponse summary = transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request);

        assertNotNull(summary);
        assertEquals(0, new BigDecimal("50.00").compareTo(summary.getVoucherAmountUsed()));
        assertEquals(0, new BigDecimal("30.00").compareTo(summary.getWalletAmountUsed()));

        // Wallet deducted by 30.00 (500.00 - 30.00 = 470.00)
        Wallet updatedWallet = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("470.00").compareTo(updatedWallet.getBalance()));
    }

    @Test
    @DisplayName("Booking fails when requested seats exceed available capacity")
    void testBookingFailsWhenOverbooked() {
        BookingRequest request = new BookingRequest(testTrip.getId(), 35, PaymentMethod.WALLET, null); // Only 30 available
        assertThrows(SeatOverbookedException.class, () -> transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request));
    }

    @Test
    @DisplayName("Booking fails when wallet balance is insufficient")
    void testBookingFailsWhenInsufficientBalance() {
        Wallet wallet = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        wallet.setBalance(new BigDecimal("10.00"));
        walletRepository.save(wallet);

        BookingRequest request = new BookingRequest(testTrip.getId(), 1, PaymentMethod.WALLET, null); // fare = 40.00
        assertThrows(InsufficientBalanceException.class, () -> transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request));
    }

    @Test
    @DisplayName("Cancel booking replenishes trip available seats and issues 100% wallet refund")
    void testCancelBookingWithRefund() {
        BookingRequest request = new BookingRequest(testTrip.getId(), 2, PaymentMethod.WALLET, null); // 80.00
        BookingSummaryResponse booking = transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request);

        // Pre-cancel checks
        Trip bookedTrip = tripRepository.findById(testTrip.getId()).orElseThrow();
        assertEquals(28, bookedTrip.getAvailableSeats());

        // Cancel
        BookingSummaryResponse cancelled = transitService.cancelBooking(booking.getId(), commuter.getId());
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        // Seats replenished to 30
        Trip replenishedTrip = tripRepository.findById(testTrip.getId()).orElseThrow();
        assertEquals(30, replenishedTrip.getAvailableSeats());

        // Wallet refunded 80.00 back to 500.00
        Wallet refundedWallet = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(refundedWallet.getBalance()));
    }

    @Test
    @DisplayName("Cancel booking fails if unauthorized user attempts cancellation")
    void testCancelBookingUnauthorizedFails() {
        BookingRequest request = new BookingRequest(testTrip.getId(), 1, PaymentMethod.WALLET, null);
        BookingSummaryResponse booking = transitService.bookSeatsWithRetry(testTrip.getId(), commuter.getId(), request);

        assertThrows(BusinessException.class, () -> transitService.cancelBooking(booking.getId(), admin.getId()));
    }
}
