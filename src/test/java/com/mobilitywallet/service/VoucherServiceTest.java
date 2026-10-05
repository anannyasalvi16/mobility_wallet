package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.VoucherBulkCreateRequest;
import com.mobilitywallet.dto.request.VoucherCreateRequest;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.exception.InvalidVoucherStateException;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrCodeService qrCodeService;

    private VoucherService voucherService;

    private User admin;
    private User commuter;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherServiceImpl(voucherRepository, userRepository, qrCodeService);

        admin = new User("Admin", "admin@mobility.com", "hash", "111", Role.ROLE_ADMIN);
        admin.setId(1L);

        commuter = new User("Commuter", "user@mobility.com", "hash", "222", Role.ROLE_USER);
        commuter.setId(2L);
    }

    @Test
    @DisplayName("Create single voucher: Generates VCH- code and status CREATED")
    void createSingleVoucher() {
        VoucherCreateRequest request = new VoucherCreateRequest(new BigDecimal("500.00"), LocalDate.now().plusMonths(1));
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voucher voucher = voucherService.createVoucher(request, admin);

        assertThat(voucher.getCode()).startsWith("VCH-");
        assertThat(voucher.getInitialValue()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.CREATED);
        assertThat(voucher.getCreatedBy()).isEqualTo(admin);
        assertThat(voucher.getAssignedTo()).isNull();
    }

    @Test
    @DisplayName("Create bulk vouchers: Generates N unique vouchers in a batch")
    @SuppressWarnings("unchecked")
    void createBulkVouchers() {
        VoucherBulkCreateRequest request = new VoucherBulkCreateRequest(5, new BigDecimal("250.00"), LocalDate.now().plusWeeks(2));
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        when(voucherRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Voucher> batch = voucherService.createBulkVouchers(request, admin);

        assertThat(batch).hasSize(5);
        for (Voucher v : batch) {
            assertThat(v.getCode()).startsWith("VCH-");
            assertThat(v.getStatus()).isEqualTo(VoucherStatus.CREATED);
            assertThat(v.getInitialValue()).isEqualByComparingTo(new BigDecimal("250.00"));
        }
    }

    @Test
    @DisplayName("Assign voucher: Transitions status from CREATED to ASSIGNED and links user")
    void assignVoucherSuccess() {
        Voucher voucher = new Voucher("VCH-TEST-001", new BigDecimal("300.00"), LocalDate.now().plusDays(10), admin);
        voucher.setId(101L);

        when(voucherRepository.findById(101L)).thenReturn(Optional.of(voucher));
        when(userRepository.findById(2L)).thenReturn(Optional.of(commuter));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voucher assigned = voucherService.assignVoucher(101L, 2L);

        assertThat(assigned.getStatus()).isEqualTo(VoucherStatus.ASSIGNED);
        assertThat(assigned.getAssignedTo()).isEqualTo(commuter);
    }

    @Test
    @DisplayName("Assigning expired voucher marks it EXPIRED and throws InvalidVoucherStateException")
    void assignExpiredVoucherThrows() {
        Voucher pastDueVoucher = new Voucher("VCH-PAST-002", new BigDecimal("300.00"), LocalDate.now().minusDays(1), admin);
        pastDueVoucher.setId(102L);

        when(voucherRepository.findById(102L)).thenReturn(Optional.of(pastDueVoucher));

        assertThatThrownBy(() -> voucherService.assignVoucher(102L, 2L))
                .isInstanceOf(InvalidVoucherStateException.class)
                .hasMessageContaining("Cannot assign an expired voucher");

        assertThat(pastDueVoucher.getStatus()).isEqualTo(VoucherStatus.EXPIRED);
        verify(voucherRepository).save(pastDueVoucher);
    }

    @Test
    @DisplayName("Assigning voucher not in CREATED status throws InvalidVoucherStateException")
    void assignAlreadyAssignedVoucherThrows() {
        Voucher alreadyAssigned = new Voucher("VCH-ASSIGNED-003", new BigDecimal("300.00"), LocalDate.now().plusDays(5), admin);
        alreadyAssigned.setId(103L);
        alreadyAssigned.setStatus(VoucherStatus.ASSIGNED);

        when(voucherRepository.findById(103L)).thenReturn(Optional.of(alreadyAssigned));

        assertThatThrownBy(() -> voucherService.assignVoucher(103L, 2L))
                .isInstanceOf(InvalidVoucherStateException.class)
                .hasMessageContaining("cannot be assigned");
    }

    @Test
    @DisplayName("Get active vouchers for user: Lazily expires past-due vouchers and returns only valid ones")
    void getActiveVouchersLazilyExpiresPastDue() {
        Voucher validVoucher = new Voucher("VCH-VALID", new BigDecimal("400.00"), LocalDate.now().plusDays(5), admin);
        validVoucher.setStatus(VoucherStatus.ASSIGNED);

        Voucher expiredVoucher = new Voucher("VCH-EXPIRED", new BigDecimal("200.00"), LocalDate.now().minusDays(2), admin);
        expiredVoucher.setStatus(VoucherStatus.ASSIGNED);

        List<Voucher> userVouchers = new ArrayList<>(List.of(validVoucher, expiredVoucher));
        when(voucherRepository.findByAssignedToIdAndStatusOrderByExpiryDateAsc(2L, VoucherStatus.ASSIGNED))
                .thenReturn(userVouchers);

        List<Voucher> active = voucherService.getActiveVouchersForUser(2L);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getCode()).isEqualTo("VCH-VALID");
        assertThat(expiredVoucher.getStatus()).isEqualTo(VoucherStatus.EXPIRED);
        verify(voucherRepository).save(expiredVoucher);
    }

    @Test
    @DisplayName("Expire past due vouchers sweep transitions ASSIGNED and CREATED vouchers to EXPIRED")
    void expirePastDueVouchersSweep() {
        LocalDate today = LocalDate.now();
        Voucher v1 = new Voucher("VCH-PAST1", new BigDecimal("100.00"), today.minusDays(1), admin);
        v1.setStatus(VoucherStatus.ASSIGNED);

        Voucher v2 = new Voucher("VCH-PAST2", new BigDecimal("100.00"), today.minusDays(3), admin);
        v2.setStatus(VoucherStatus.CREATED);

        when(voucherRepository.findByStatusAndExpiryDateBefore(VoucherStatus.ASSIGNED, today)).thenReturn(List.of(v1));
        when(voucherRepository.findByStatusAndExpiryDateBefore(VoucherStatus.CREATED, today)).thenReturn(List.of(v2));

        int count = voucherService.expirePastDueVouchers();

        assertThat(count).isEqualTo(2);
        assertThat(v1.getStatus()).isEqualTo(VoucherStatus.EXPIRED);
        assertThat(v2.getStatus()).isEqualTo(VoucherStatus.EXPIRED);
        verify(voucherRepository, times(2)).save(any(Voucher.class));
    }
}
