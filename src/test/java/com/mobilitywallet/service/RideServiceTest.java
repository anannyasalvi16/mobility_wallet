package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.dto.response.RideResponse;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InvalidVoucherStateException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.exception.VoucherAlreadyRedeemedException;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RideServiceTest {

    @Autowired
    private RideService rideService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User commuter;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin-ride-test@mobility.com").orElseGet(() -> {
            User a = new User("Admin Test", "admin-ride-test@mobility.com", passwordEncoder.encode("admin123"), "9111100000", Role.ROLE_ADMIN);
            Wallet w = new Wallet(a, BigDecimal.ZERO);
            a.setWallet(w);
            return userRepository.save(a);
        });

        commuter = userRepository.findByEmail("user-ride-test@mobility.com").orElseGet(() -> {
            User c = new User("Commuter Test", "user-ride-test@mobility.com", passwordEncoder.encode("user123"), "9111122222", Role.ROLE_USER);
            Wallet w = new Wallet(c, new BigDecimal("500.00"));
            c.setWallet(w);
            return userRepository.save(c);
        });
    }

    private Voucher createAssignedVoucher(String code, BigDecimal value, LocalDate expiry) {
        Voucher v = new Voucher(code, value, expiry, admin);
        v.setAssignedTo(commuter);
        v.setStatus(VoucherStatus.ASSIGNED);
        return voucherRepository.save(v);
    }

    @Test
    @DisplayName("Successfully redeem voucher for taxi ride, updating voucher state, creating ride, and appending ledger entry")
    void testSuccessfulRedemption() {
        Voucher v = createAssignedVoucher("VCH-TEST-SUCCESS-1", new BigDecimal("600.00"), LocalDate.now().plusMonths(1));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Suresh Kumar",
                "MH-04-AX-5555",
                "Andheri Station",
                "Powai Tech Park",
                new BigDecimal("420.00")
        );

        RideResponse response = rideService.redeemVoucher(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("VCH-TEST-SUCCESS-1", response.getVoucherCode());
        assertEquals("Suresh Kumar", response.getDriverName());
        assertEquals("MH-04-AX-5555", response.getVehicleNumber());
        assertEquals(0, new BigDecimal("420.00").compareTo(response.getFare()));
        assertEquals(commuter.getEmail(), response.getUserEmail());

        // Verify voucher status transitioned to REDEEMED
        Voucher updatedVoucher = voucherRepository.findByCode(v.getCode()).orElseThrow();
        assertEquals(VoucherStatus.REDEEMED, updatedVoucher.getStatus());
        assertNotNull(updatedVoucher.getRedeemedAt());

        // Verify transaction ledger
        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(tx -> tx.getVoucher() != null && v.getCode().equals(tx.getVoucher().getCode()))
                .toList();
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.DEBIT, transactions.get(0).getType());
        assertEquals(0, new BigDecimal("420.00").compareTo(transactions.get(0).getAmount()));
    }

    @Test
    @DisplayName("Redemption throws ResourceNotFoundException for invalid voucher code")
    void testVoucherNotFound() {
        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "NON-EXISTENT-VOUCHER",
                "Driver",
                "MH-01-AA-0001",
                "A",
                "B",
                new BigDecimal("100.00")
        );

        assertThrows(ResourceNotFoundException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Redemption throws InvalidVoucherStateException if voucher is not ASSIGNED (e.g. CREATED)")
    void testUnassignedVoucherRedemptionFails() {
        Voucher v = new Voucher("VCH-TEST-CREATED-1", new BigDecimal("500.00"), LocalDate.now().plusMonths(1), admin);
        v = voucherRepository.save(v);

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Driver",
                "MH-01-AA-0001",
                "A",
                "B",
                new BigDecimal("200.00")
        );

        assertThrows(InvalidVoucherStateException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Redemption throws BusinessException if voucher is expired")
    void testExpiredVoucherRedemptionFails() {
        Voucher v = createAssignedVoucher("VCH-TEST-EXPIRED-1", new BigDecimal("500.00"), LocalDate.now().minusDays(1));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Driver",
                "MH-01-AA-0001",
                "A",
                "B",
                new BigDecimal("200.00")
        );

        assertThrows(BusinessException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Redemption throws VoucherAlreadyRedeemedException if already redeemed")
    void testAlreadyRedeemedVoucherFails() {
        Voucher v = createAssignedVoucher("VCH-TEST-ALREADY-1", new BigDecimal("500.00"), LocalDate.now().plusMonths(1));
        v.setStatus(VoucherStatus.REDEEMED);
        voucherRepository.save(v);

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Driver",
                "MH-01-AA-0001",
                "A",
                "B",
                new BigDecimal("200.00")
        );

        assertThrows(VoucherAlreadyRedeemedException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Redemption throws BusinessException if fare exceeds voucher initial value")
    void testFareExceedingVoucherValueFails() {
        Voucher v = createAssignedVoucher("VCH-TEST-EXCEED-1", new BigDecimal("300.00"), LocalDate.now().plusMonths(1));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Driver",
                "MH-01-AA-0001",
                "A",
                "B",
                new BigDecimal("350.00") // exceeds 300.00
        );

        assertThrows(BusinessException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Redemption throws BusinessException if fare is zero or negative")
    void testInvalidFareFails() {
        Voucher v = createAssignedVoucher("VCH-TEST-ZERO-1", new BigDecimal("300.00"), LocalDate.now().plusMonths(1));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Driver",
                "MH-01-AA-0001",
                "A",
                "B",
                BigDecimal.ZERO
        );

        assertThrows(BusinessException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Querying ride by ID, voucher code, and user ID succeeds")
    void testQueryMethods() {
        Voucher v = createAssignedVoucher("VCH-TEST-QUERY-1", new BigDecimal("500.00"), LocalDate.now().plusMonths(1));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Vikram Patel",
                "MH-03-ZZ-9999",
                "Colaba",
                "Dadar",
                new BigDecimal("250.00")
        );

        RideResponse created = rideService.redeemVoucher(request);

        RideResponse byId = rideService.getRideById(created.getId());
        assertEquals(created.getId(), byId.getId());

        RideResponse byCode = rideService.getRideByVoucherCode(v.getCode());
        assertEquals(created.getId(), byCode.getId());

        List<RideResponse> userRides = rideService.getRidesByUserId(commuter.getId());
        assertEquals(1, userRides.size());

        Page<RideResponse> allRides = rideService.getAllRides(PageRequest.of(0, 10));
        assertNotNull(allRides);
    }
}
