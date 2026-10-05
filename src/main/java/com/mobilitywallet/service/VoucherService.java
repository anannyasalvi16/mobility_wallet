package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.VoucherBulkCreateRequest;
import com.mobilitywallet.dto.request.VoucherCreateRequest;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VoucherService {

    Voucher createVoucher(VoucherCreateRequest request, User creator);

    List<Voucher> createBulkVouchers(VoucherBulkCreateRequest request, User creator);

    Voucher assignVoucher(Long voucherId, Long userId);

    Voucher findByCode(String code);

    Voucher findById(Long id);

    List<Voucher> getVouchersForUser(Long userId);

    List<Voucher> getActiveVouchersForUser(Long userId);

    Page<Voucher> getAllVouchers(Pageable pageable);

    String getVoucherQrCode(String code);

    int expirePastDueVouchers();
}
