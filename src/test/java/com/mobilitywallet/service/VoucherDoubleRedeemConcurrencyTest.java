package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.VoucherAlreadyRedeemedException;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class VoucherDoubleRedeemConcurrencyTest {

    private static final String CONCURRENT_VOUCHER_CODE = "VCH-CONCURRENT-001";

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
        cleanUpTestData();

        admin = userRepository.findByEmail("admin@mobility.com").orElseGet(() -> {
            User a = new User("Admin", "admin@mobility.com", passwordEncoder.encode("admin123"), "9999900000", Role.ROLE_ADMIN);
            Wallet w = new Wallet(a, BigDecimal.ZERO);
            a.setWallet(w);
            return userRepository.save(a);
        });

        commuter = userRepository.findByEmail("user@mobility.com").orElseGet(() -> {
            User c = new User("Commuter", "user@mobility.com", passwordEncoder.encode("user123"), "9999911111", Role.ROLE_USER);
            Wallet w = new Wallet(c, new BigDecimal("750.00"));
            c.setWallet(w);
            return userRepository.save(c);
        });

        // Create a single ASSIGNED voucher ready for redemption
        Voucher voucher = new Voucher(CONCURRENT_VOUCHER_CODE, new BigDecimal("500.00"), LocalDate.now().plusMonths(1), admin);
        voucher.setAssignedTo(commuter);
        voucher.setStatus(VoucherStatus.ASSIGNED);
        voucherRepository.saveAndFlush(voucher);
    }

    @AfterEach
    void tearDown() {
        cleanUpTestData();
    }

    private void cleanUpTestData() {
        // Delete ledger and ride records associated with the test voucher
        transactionRepository.findByVoucherCode(CONCURRENT_VOUCHER_CODE).forEach(transactionRepository::delete);

        rideRepository.findByVoucherCode(CONCURRENT_VOUCHER_CODE).ifPresent(rideRepository::delete);
        voucherRepository.findByCode(CONCURRENT_VOUCHER_CODE).ifPresent(voucherRepository::delete);
    }

    @Test
    @DisplayName("Concurrent redemptions: exactly 1 succeeds, 9 fail with optimistic lock/already-redeemed exception, 1 ledger entry logged")
    void verifyVoucherDoubleRedemptionConcurrencyProtection() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Block until firing trigger
                    TaxiRedeemRequest request = new TaxiRedeemRequest(
                            CONCURRENT_VOUCHER_CODE,
                            "Driver " + threadIndex,
                            "MH-02-CD-" + (1000 + threadIndex),
                            "Terminal 2",
                            "BKC Junction",
                            new BigDecimal("450.00")
                    );
                    rideService.redeemVoucher(request);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    Throwable root = t;
                    while (root.getCause() != null && root.getCause() != root &&
                            !(root instanceof VoucherAlreadyRedeemedException || root instanceof OptimisticLockingFailureException)) {
                        root = root.getCause();
                    }
                    if (root instanceof VoucherAlreadyRedeemedException ||
                            root instanceof OptimisticLockingFailureException ||
                            root instanceof org.hibernate.StaleObjectStateException ||
                            root instanceof DataIntegrityViolationException) {
                        conflictCount.incrementAndGet();
                    } else {
                        unexpectedExceptions.add(t);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all 10 worker threads to reach starting line
        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All 10 threads must reach the starting line within 5 seconds");

        // Fire trigger: release all 10 threads simultaneously
        startLatch.countDown();

        // Wait for all 10 worker threads to finish execution
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All 10 threads must finish within 10 seconds");
        executor.shutdown();

        // Assertions
        assertTrue(unexpectedExceptions.isEmpty(), "No unexpected exceptions should occur: " + unexpectedExceptions);
        assertEquals(1, successCount.get(), "Exactly 1 thread must successfully redeem the voucher");
        assertEquals(9, conflictCount.get(), "Exactly 9 threads must fail due to optimistic locking / already redeemed conflict");

        // Mathematical verification of DB state
        Voucher finalVoucher = voucherRepository.findByCode(CONCURRENT_VOUCHER_CODE).orElseThrow();
        assertEquals(VoucherStatus.REDEEMED, finalVoucher.getStatus(), "Voucher must be in REDEEMED status");
        assertNotNull(finalVoucher.getRedeemedAt(), "Voucher redeemedAt timestamp must be populated");

        assertTrue(rideRepository.findByVoucherCode(CONCURRENT_VOUCHER_CODE).isPresent(),
                "Exactly 1 Ride record must exist for the redeemed voucher");

        List<Transaction> voucherLedger = transactionRepository.findByVoucherCode(CONCURRENT_VOUCHER_CODE);
        assertEquals(1, voucherLedger.size(), "Ledger must contain exactly 1 debit entry for the voucher");
        assertEquals(TransactionType.DEBIT, voucherLedger.get(0).getType());
        assertEquals(0, new BigDecimal("450.00").compareTo(voucherLedger.get(0).getAmount()));
    }
}
