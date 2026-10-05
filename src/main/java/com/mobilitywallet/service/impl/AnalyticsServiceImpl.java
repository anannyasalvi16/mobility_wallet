package com.mobilitywallet.service.impl;

import com.mobilitywallet.dto.response.AdminAnalyticsDto;
import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import com.mobilitywallet.service.AnalyticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final TransactionRepository transactionRepository;

    public AnalyticsServiceImpl(VoucherRepository voucherRepository,
                                UserRepository userRepository,
                                WalletRepository walletRepository,
                                BookingRepository bookingRepository,
                                RideRepository rideRepository,
                                RouteRepository routeRepository,
                                TripRepository tripRepository,
                                TransactionRepository transactionRepository) {
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public AdminAnalyticsDto getAdminAnalytics() {
        AdminAnalyticsDto dto = new AdminAnalyticsDto();

        long created = voucherRepository.countByStatus(VoucherStatus.CREATED);
        long assigned = voucherRepository.countByStatus(VoucherStatus.ASSIGNED);
        long redeemed = voucherRepository.countByStatus(VoucherStatus.REDEEMED);
        long expired = voucherRepository.countByStatus(VoucherStatus.EXPIRED);
        long totalVouchers = voucherRepository.count();

        dto.setCreatedVouchersCount(created);
        dto.setAssignedVouchersCount(assigned);
        dto.setRedeemedVouchersCount(redeemed);
        dto.setExpiredVouchersCount(expired);
        dto.setTotalVouchersCount(totalVouchers);

        BigDecimal totalIssued = voucherRepository.findAll().stream()
                .map(Voucher::getInitialValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalIssuedAmount(totalIssued != null ? totalIssued.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        BigDecimal totalRedeemed = voucherRepository.sumInitialValueByStatus(VoucherStatus.REDEEMED);
        dto.setTotalRedeemedAmount(totalRedeemed != null ? totalRedeemed.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        BigDecimal totalWallet = walletRepository.sumAllBalances();
        dto.setTotalWalletBalance(totalWallet != null ? totalWallet.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        dto.setTotalBookingsCount(bookingRepository.count());
        dto.setTotalRidesCount(rideRepository.count());
        dto.setTotalUsersCount(userRepository.count());

        // Daily spend (past 7 days)
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MMM dd");
        List<String> spendLabels = new ArrayList<>();
        List<BigDecimal> spendValues = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            spendLabels.add(date.format(dayFormatter));
            BigDecimal dailyDebit = transactionRepository.sumDebitAmountBetween(start, end);
            spendValues.add(dailyDebit != null ? dailyDebit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }
        dto.setDailySpendLabels(spendLabels);
        dto.setDailySpendValues(spendValues);

        // Route Revenues & Seat Load Factors
        List<Route> routes = routeRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();
        List<Trip> allTrips = tripRepository.findAll();

        List<String> routeLabels = new ArrayList<>();
        List<BigDecimal> routeRevenues = new ArrayList<>();
        List<Double> routeLoadFactors = new ArrayList<>();

        for (Route route : routes) {
            String label = route.getOrigin() + " → " + route.getDestination() + " (" + route.getMode() + ")";
            routeLabels.add(label);

            // Compute gross confirmed revenue for this route
            BigDecimal routeRev = allBookings.stream()
                    .filter(b -> b.getTrip() != null && b.getTrip().getRoute() != null && b.getTrip().getRoute().getId().equals(route.getId()))
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .map(Booking::getTotalFare)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            routeRevenues.add(routeRev.setScale(2, RoundingMode.HALF_UP));

            // Compute load factor
            List<Trip> routeTrips = allTrips.stream()
                    .filter(t -> t.getRoute() != null && t.getRoute().getId().equals(route.getId()))
                    .toList();
            int totalSeats = routeTrips.stream().mapToInt(Trip::getTotalSeats).sum();
            int availableSeats = routeTrips.stream().mapToInt(Trip::getAvailableSeats).sum();
            int bookedSeats = totalSeats - availableSeats;

            double loadFactor = (totalSeats > 0) ? ((double) bookedSeats / totalSeats) * 100.0 : 0.0;
            routeLoadFactors.add(Math.round(loadFactor * 10.0) / 10.0);
        }

        dto.setRouteLabels(routeLabels);
        dto.setRouteRevenues(routeRevenues);
        dto.setRouteLoadFactors(routeLoadFactors);

        return dto;
    }

    @Override
    public List<Transaction> getRecentTransactions(int limit) {
        int pageSize = limit > 0 ? limit : 5;
        return transactionRepository.findTopWithDetailsOrderByCreatedAtDesc(PageRequest.of(0, pageSize));
    }
}
