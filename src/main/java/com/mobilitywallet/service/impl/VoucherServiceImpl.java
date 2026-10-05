package com.mobilitywallet.service.impl;

import com.mobilitywallet.dto.request.VoucherBulkCreateRequest;
import com.mobilitywallet.dto.request.VoucherCreateRequest;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.exception.InvalidVoucherStateException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.service.QrCodeService;
import com.mobilitywallet.service.VoucherService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    public VoucherServiceImpl(VoucherRepository voucherRepository,
                              UserRepository userRepository,
                              QrCodeService qrCodeService) {
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
    }

    private String generateUniqueCode() {
        int currentYear = Year.now().getValue();
        String code;
        do {
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            code = String.format("VCH-%d-%s", currentYear, randomPart);
        } while (voucherRepository.existsByCode(code));
        return code;
    }

    @Override
    @Transactional
    public Voucher createVoucher(VoucherCreateRequest request, User creator) {
        String code = generateUniqueCode();
        Voucher voucher = new Voucher(code, request.getInitialValue(), request.getExpiryDate(), creator);
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public List<Voucher> createBulkVouchers(VoucherBulkCreateRequest request, User creator) {
        List<Voucher> batch = new ArrayList<>(request.getCount());
        for (int i = 0; i < request.getCount(); i++) {
            String code = generateUniqueCode();
            Voucher voucher = new Voucher(code, request.getInitialValue(), request.getExpiryDate(), creator);
            batch.add(voucher);
        }
        return voucherRepository.saveAll(batch);
    }

    @Override
    @Transactional
    public Voucher assignVoucher(Long voucherId, Long userId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", voucherId));

        if (voucher.isExpired()) {
            voucher.setStatus(VoucherStatus.EXPIRED);
            voucherRepository.save(voucher);
            throw new InvalidVoucherStateException("Cannot assign an expired voucher.");
        }

        if (voucher.getStatus() != VoucherStatus.CREATED) {
            throw new InvalidVoucherStateException(
                    String.format("Voucher %s is in %s state and cannot be assigned.", voucher.getCode(), voucher.getStatus())
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        voucher.setAssignedTo(user);
        voucher.setStatus(VoucherStatus.ASSIGNED);
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public Voucher findByCode(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "code", code));

        // Lazy check: if expired and not yet marked or redeemed, update status
        if (voucher.isExpired() && voucher.getStatus() != VoucherStatus.REDEEMED && voucher.getStatus() != VoucherStatus.EXPIRED) {
            voucher.setStatus(VoucherStatus.EXPIRED);
            return voucherRepository.save(voucher);
        }

        return voucher;
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher findById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", id));
    }

    @Override
    @Transactional
    public List<Voucher> getVouchersForUser(Long userId) {
        List<Voucher> vouchers = voucherRepository.findByAssignedToIdOrderByExpiryDateAsc(userId);
        // Check and lazily update any that expired
        for (Voucher v : vouchers) {
            if (v.isExpired() && v.getStatus() == VoucherStatus.ASSIGNED) {
                v.setStatus(VoucherStatus.EXPIRED);
                voucherRepository.save(v);
            }
        }
        return vouchers;
    }

    @Override
    @Transactional
    public List<Voucher> getActiveVouchersForUser(Long userId) {
        List<Voucher> assignedVouchers = voucherRepository.findByAssignedToIdAndStatusOrderByExpiryDateAsc(userId, VoucherStatus.ASSIGNED);
        List<Voucher> active = new ArrayList<>();
        for (Voucher v : assignedVouchers) {
            if (v.isExpired()) {
                v.setStatus(VoucherStatus.EXPIRED);
                voucherRepository.save(v);
            } else {
                active.add(v);
            }
        }
        return active;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Voucher> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public String getVoucherQrCode(String code) {
        Voucher voucher = findByCode(code);
        String payload = qrCodeService.generateVoucherPayload(voucher);
        return qrCodeService.generateQrCodeDataUri(payload, 250, 250);
    }

    @Override
    @Transactional
    public int expirePastDueVouchers() {
        LocalDate today = LocalDate.now();
        List<Voucher> assignedPastDue = voucherRepository.findByStatusAndExpiryDateBefore(VoucherStatus.ASSIGNED, today);
        List<Voucher> createdPastDue = voucherRepository.findByStatusAndExpiryDateBefore(VoucherStatus.CREATED, today);

        int count = 0;
        for (Voucher v : assignedPastDue) {
            v.setStatus(VoucherStatus.EXPIRED);
            voucherRepository.save(v);
            count++;
        }
        for (Voucher v : createdPastDue) {
            v.setStatus(VoucherStatus.EXPIRED);
            voucherRepository.save(v);
            count++;
        }
        return count;
    }
}
