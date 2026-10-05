package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Ride;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Ticket;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EntityAndRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private RideRepository rideRepository;

    @Test
    @DisplayName("User and Wallet: Persistence, OneToOne relationship, and @Version increment")
    void testUserAndWalletPersistenceAndVersion() {
        User user = new User("Alice Kumar", "alice@example.com", "secureHash", "9876543210", Role.ROLE_USER);
        Wallet wallet = new Wallet(user, new BigDecimal("1000.00"));
        user.setWallet(wallet);

        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<Wallet> foundWalletOpt = walletRepository.findByUserEmail("alice@example.com");
        assertThat(foundWalletOpt).isPresent();
        Wallet foundWallet = foundWalletOpt.get();
        assertThat(foundWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(foundWallet.getVersion()).isEqualTo(0L);

        // Update balance and verify @Version increments
        foundWallet.setBalance(new BigDecimal("850.50"));
        walletRepository.saveAndFlush(foundWallet);
        entityManager.clear();

        Wallet updatedWallet = walletRepository.findById(foundWallet.getId()).orElseThrow();
        assertThat(updatedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("850.50"));
        assertThat(updatedWallet.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Voucher: Creation, Assignment, and @Version optimistic lock increment")
    void testVoucherLifecycleAndVersion() {
        User admin = userRepository.save(new User("Admin User", "admin@corp.com", "hash", "111", Role.ROLE_ADMIN));
        User user = userRepository.save(new User("Bob Smith", "bob@corp.com", "hash", "222", Role.ROLE_USER));
        entityManager.flush();

        Voucher voucher = new Voucher("VCH-2026-TEST01", new BigDecimal("500.00"), LocalDate.now().plusMonths(1), admin);
        voucherRepository.save(voucher);
        entityManager.flush();
        entityManager.clear();

        Voucher foundVoucher = voucherRepository.findByCode("VCH-2026-TEST01").orElseThrow();
        assertThat(foundVoucher.getStatus()).isEqualTo(VoucherStatus.CREATED);
        assertThat(foundVoucher.getVersion()).isEqualTo(0L);

        // Assign to user
        foundVoucher.setAssignedTo(user);
        foundVoucher.setStatus(VoucherStatus.ASSIGNED);
        voucherRepository.saveAndFlush(foundVoucher);
        entityManager.clear();

        Voucher assignedVoucher = voucherRepository.findByCode("VCH-2026-TEST01").orElseThrow();
        assertThat(assignedVoucher.getStatus()).isEqualTo(VoucherStatus.ASSIGNED);
        assertThat(assignedVoucher.getAssignedTo().getId()).isEqualTo(user.getId());
        assertThat(assignedVoucher.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Route and Trip: Available trip search and @Version on Trip")
    void testRouteAndTripSearchAndVersion() {
        Route route = routeRepository.save(new Route("Campus", "City Center", 15.5, TransitMode.BUS));
        LocalDateTime departure = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime arrival = departure.plusHours(1);

        Trip trip = new Trip(route, departure, arrival, 40, new BigDecimal("45.00"));
        tripRepository.save(trip);
        entityManager.flush();
        entityManager.clear();

        // Search trip by origin, destination and time window
        List<Trip> trips = tripRepository.searchAvailableTrips(
                "Campus",
                "City Center",
                departure.minusHours(1),
                departure.plusHours(1)
        );

        assertThat(trips).hasSize(1);
        Trip foundTrip = trips.get(0);
        assertThat(foundTrip.getAvailableSeats()).isEqualTo(40);
        assertThat(foundTrip.getFare()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(foundTrip.getVersion()).isEqualTo(0L);

        // Decrement seat (simulating a booking) and verify version increment
        foundTrip.setAvailableSeats(39);
        tripRepository.saveAndFlush(foundTrip);
        entityManager.clear();

        Trip updatedTrip = tripRepository.findById(foundTrip.getId()).orElseThrow();
        assertThat(updatedTrip.getAvailableSeats()).isEqualTo(39);
        assertThat(updatedTrip.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Booking and Ticket: Cascade persistence and relationship")
    void testBookingAndTicketCascade() {
        User user = userRepository.save(new User("Charlie", "charlie@corp.com", "hash", "333", Role.ROLE_USER));
        Route route = routeRepository.save(new Route("Metro Station A", "Metro Station B", 8.0, TransitMode.METRO));
        Trip trip = tripRepository.save(new Trip(route, LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), 60, new BigDecimal("25.00")));

        Booking booking = new Booking("BKG-REF-1001", user, trip, 2, new BigDecimal("50.00"), PaymentMethod.WALLET);
        Ticket ticket1 = new Ticket(booking, "A1", "TKT-1001-1", "QR_DATA_A1");
        Ticket ticket2 = new Ticket(booking, "A2", "TKT-1001-2", "QR_DATA_A2");
        booking.addTicket(ticket1);
        booking.addTicket(ticket2);

        bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();

        Booking foundBooking = bookingRepository.findByBookingReference("BKG-REF-1001").orElseThrow();
        assertThat(foundBooking.getTickets()).hasSize(2);
        assertThat(foundBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(foundBooking.getTotalFare()).isEqualByComparingTo(new BigDecimal("50.00"));

        List<Ticket> userTickets = ticketRepository.findByBookingUserIdOrderByBookingBookingDateDesc(user.getId());
        assertThat(userTickets).hasSize(2);
    }

    @Test
    @DisplayName("Ride: Taxi ride linked to redeemed voucher")
    void testRidePersistenceWithVoucher() {
        User admin = userRepository.save(new User("Admin", "admin2@corp.com", "hash", "444", Role.ROLE_ADMIN));
        User user = userRepository.save(new User("Diana", "diana@corp.com", "hash", "555", Role.ROLE_USER));
        Voucher voucher = voucherRepository.save(new Voucher("VCH-TAXI-001", new BigDecimal("350.00"), LocalDate.now().plusDays(10), admin));
        voucher.setStatus(VoucherStatus.REDEEMED);
        voucher.setRedeemedAt(LocalDateTime.now());
        voucherRepository.save(voucher);

        Ride ride = new Ride(user, voucher, "Ramesh Driver", "MH-01-AB-1234", "Office Gate 1", "Airport T2", new BigDecimal("320.00"));
        rideRepository.save(ride);
        entityManager.flush();
        entityManager.clear();

        Optional<Ride> foundRide = rideRepository.findByVoucherCode("VCH-TAXI-001");
        assertThat(foundRide).isPresent();
        assertThat(foundRide.get().getDriverName()).isEqualTo("Ramesh Driver");
        assertThat(foundRide.get().getFare()).isEqualByComparingTo(new BigDecimal("320.00"));
    }

    @Test
    @DisplayName("Transaction: Immutable ledger credit and debit entries")
    void testTransactionLedger() {
        User user = userRepository.save(new User("Evan", "evan@corp.com", "hash", "666", Role.ROLE_USER));
        Wallet wallet = walletRepository.save(new Wallet(user, new BigDecimal("500.00")));

        Transaction creditTx = new Transaction(wallet, null, TransactionType.CREDIT, new BigDecimal("500.00"), "Initial voucher allowance credit");
        Transaction debitTx = new Transaction(wallet, null, TransactionType.DEBIT, new BigDecimal("120.00"), "Bus ticket booking fare");
        transactionRepository.save(creditTx);
        transactionRepository.save(debitTx);
        entityManager.flush();
        entityManager.clear();

        List<Transaction> txList = transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(txList).hasSize(2);

        BigDecimal totalDebits = transactionRepository.sumDebitAmountSince(LocalDateTime.now().minusDays(1));
        assertThat(totalDebits).isEqualByComparingTo(new BigDecimal("120.00"));
    }
}
