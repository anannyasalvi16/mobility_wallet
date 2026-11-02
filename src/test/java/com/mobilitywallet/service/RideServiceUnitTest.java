package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.dto.response.RideResponse;
import com.mobilitywallet.entity.Ride;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InsufficientBalanceException;
import com.mobilitywallet.exception.VoucherAlreadyRedeemedException;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import com.mobilitywallet.service.impl.RideServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceUnitTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RideServiceImpl rideService;

    private User commuter;
    private User admin;
    private Voucher voucher;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        admin = new User("Admin", "admin@mobility.com", "hash", "9000000000", Role.ROLE_ADMIN);
        admin.setId(1L);

        commuter = new User("Commuter", "user@mobility.com", "hash", "9000000001", Role.ROLE_USER);
        commuter.setId(2L);

        wallet = new Wallet(commuter, new BigDecimal("500.00"));
        wallet.setId(10L);

        voucher = new Voucher("VCH-UNIT-001", new BigDecimal("300.00"), LocalDate.now().plusMonths(1), admin);
        voucher.setId(100L);
        voucher.setAssignedTo(commuter);
        voucher.setStatus(VoucherStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Unit: Successfully redeem voucher for ride")
    void testRedeemVoucher_Success() {
        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "VCH-UNIT-001", "Ramesh", "MH-01-AB-1234", "Point A", "Point B", new BigDecimal("250.00")
        );

        when(voucherRepository.findByCode("VCH-UNIT-001")).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rideRepository.saveAndFlush(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });
        when(walletRepository.findByUserId(commuter.getId())).thenReturn(Optional.of(wallet));

        RideResponse response = rideService.redeemVoucher(request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("VCH-UNIT-001", response.getVoucherCode());
        assertEquals(0, new BigDecimal("250.00").compareTo(response.getFare()));
        assertEquals(VoucherStatus.REDEEMED, voucher.getStatus());
        assertNotNull(voucher.getRedeemedAt());

        verify(voucherRepository).saveAndFlush(voucher);
        verify(rideRepository).saveAndFlush(any(Ride.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Unit: Fails when voucher is already redeemed")
    void testRedeemVoucher_AlreadyUsed() {
        voucher.setStatus(VoucherStatus.REDEEMED);
        when(voucherRepository.findByCode("VCH-UNIT-001")).thenReturn(Optional.of(voucher));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "VCH-UNIT-001", "Ramesh", "MH-01-AB-1234", "Point A", "Point B", new BigDecimal("250.00")
        );

        assertThrows(VoucherAlreadyRedeemedException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Unit: Fails when voucher is expired")
    void testRedeemVoucher_Expired() {
        voucher.setExpiryDate(LocalDate.now().minusDays(1));
        when(voucherRepository.findByCode("VCH-UNIT-001")).thenReturn(Optional.of(voucher));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "VCH-UNIT-001", "Ramesh", "MH-01-AB-1234", "Point A", "Point B", new BigDecimal("250.00")
        );

        assertThrows(BusinessException.class, () -> rideService.redeemVoucher(request));
    }

    @Test
    @DisplayName("Unit: Fails with InsufficientBalanceException when fare exceeds voucher balance")
    void testRedeemVoucher_InsufficientBalance() {
        when(voucherRepository.findByCode("VCH-UNIT-001")).thenReturn(Optional.of(voucher));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "VCH-UNIT-001", "Ramesh", "MH-01-AB-1234", "Point A", "Point B", new BigDecimal("350.00")
        );

        InsufficientBalanceException ex = assertThrows(
                InsufficientBalanceException.class, () -> rideService.redeemVoucher(request)
        );
        assertNotNull(ex.getMessage());
    }
}

