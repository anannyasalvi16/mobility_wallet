package com.mobilitywallet.service;

import com.mobilitywallet.dto.response.AdminAnalyticsDto;
import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import com.mobilitywallet.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(
                voucherRepository,
                userRepository,
                walletRepository,
                bookingRepository,
                rideRepository,
                routeRepository,
                tripRepository,
                transactionRepository
        );
    }

    @Test
    @DisplayName("getAdminAnalytics aggregates KPIs, voucher counts, financial totals, and route stats")
    void getAdminAnalytics_AggregatesCorrectly() {
        when(voucherRepository.countByStatus(VoucherStatus.CREATED)).thenReturn(5L);
        when(voucherRepository.countByStatus(VoucherStatus.ASSIGNED)).thenReturn(10L);
        when(voucherRepository.countByStatus(VoucherStatus.REDEEMED)).thenReturn(3L);
        when(voucherRepository.countByStatus(VoucherStatus.EXPIRED)).thenReturn(2L);
        when(voucherRepository.count()).thenReturn(20L);

        User admin = new User("Admin", "admin@test.com", "hash", "111", Role.ROLE_ADMIN);
        Voucher v1 = new Voucher("VCH-1", new BigDecimal("100.00"), LocalDate.now().plusDays(10), admin);
        Voucher v2 = new Voucher("VCH-2", new BigDecimal("150.00"), LocalDate.now().plusDays(10), admin);
        when(voucherRepository.findAll()).thenReturn(List.of(v1, v2));

        when(voucherRepository.sumInitialValueByStatus(VoucherStatus.REDEEMED)).thenReturn(new BigDecimal("300.00"));
        when(walletRepository.sumAllBalances()).thenReturn(new BigDecimal("1250.00"));

        when(bookingRepository.count()).thenReturn(8L);
        when(rideRepository.count()).thenReturn(4L);
        when(userRepository.count()).thenReturn(15L);

        when(transactionRepository.sumDebitAmountBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("50.00"));

        Route route = new Route("Origin", "Destination", 15.0, TransitMode.METRO);
        route.setId(1L);
        when(routeRepository.findAll()).thenReturn(List.of(route));

        Trip trip = new Trip(route, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), 50, new BigDecimal("25.00"));
        trip.setAvailableSeats(30); // 20 booked
        when(tripRepository.findAll()).thenReturn(List.of(trip));

        User commuter = new User("Commuter", "user@test.com", "hash", "222", Role.ROLE_USER);
        Booking booking = new Booking("BKG-TEST-1", commuter, trip, 2, new BigDecimal("50.00"), PaymentMethod.WALLET);
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        AdminAnalyticsDto dto = analyticsService.getAdminAnalytics();

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalVouchersCount()).isEqualTo(20L);
        assertThat(dto.getCreatedVouchersCount()).isEqualTo(5L);
        assertThat(dto.getAssignedVouchersCount()).isEqualTo(10L);
        assertThat(dto.getRedeemedVouchersCount()).isEqualTo(3L);
        assertThat(dto.getExpiredVouchersCount()).isEqualTo(2L);
        assertThat(dto.getTotalIssuedAmount()).isEqualByComparingTo("250.00");
        assertThat(dto.getTotalRedeemedAmount()).isEqualByComparingTo("300.00");
        assertThat(dto.getTotalWalletBalance()).isEqualByComparingTo("1250.00");
        assertThat(dto.getTotalBookingsCount()).isEqualTo(8L);
        assertThat(dto.getTotalRidesCount()).isEqualTo(4L);
        assertThat(dto.getTotalUsersCount()).isEqualTo(15L);

        assertThat(dto.getDailySpendLabels()).hasSize(7);
        assertThat(dto.getDailySpendValues()).hasSize(7);
        assertThat(dto.getRouteLabels()).hasSize(1);
        assertThat(dto.getRouteRevenues()).containsExactly(new BigDecimal("50.00"));
        assertThat(dto.getRouteLoadFactors()).containsExactly(40.0); // 20 / 50 * 100 = 40%
    }

    @Test
    @DisplayName("getRecentTransactions delegates to TransactionRepository with requested limit")
    void getRecentTransactions_ReturnsTransactions() {
        User commuter = new User("Commuter", "user@test.com", "hash", "222", Role.ROLE_USER);
        Wallet wallet = new Wallet(commuter, new BigDecimal("500.00"));
        Transaction tx1 = new Transaction(wallet, null, TransactionType.CREDIT, new BigDecimal("500.00"), "Welcome bonus");
        Transaction tx2 = new Transaction(wallet, null, TransactionType.DEBIT, new BigDecimal("45.00"), "Metro ticket");

        when(transactionRepository.findTopWithDetailsOrderByCreatedAtDesc(PageRequest.of(0, 5)))
                .thenReturn(List.of(tx2, tx1));

        List<Transaction> result = analyticsService.getRecentTransactions(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isEqualTo("Metro ticket");
        assertThat(result.get(1).getDescription()).isEqualTo("Welcome bonus");
        verify(transactionRepository).findTopWithDetailsOrderByCreatedAtDesc(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("getRecentTransactions defaults to limit 5 when given 0 or negative")
    void getRecentTransactions_DefaultLimit() {
        when(transactionRepository.findTopWithDetailsOrderByCreatedAtDesc(PageRequest.of(0, 5)))
                .thenReturn(List.of());

        List<Transaction> result = analyticsService.getRecentTransactions(0);

        assertThat(result).isEmpty();
        verify(transactionRepository).findTopWithDetailsOrderByCreatedAtDesc(PageRequest.of(0, 5));
    }
}
