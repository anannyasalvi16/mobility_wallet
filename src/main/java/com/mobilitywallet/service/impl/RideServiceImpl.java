package com.mobilitywallet.service.impl;

import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.dto.response.RideResponse;
import com.mobilitywallet.entity.Ride;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InsufficientBalanceException;
import com.mobilitywallet.exception.InvalidVoucherStateException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.exception.VoucherAlreadyRedeemedException;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import com.mobilitywallet.service.RideService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final VoucherRepository voucherRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public RideServiceImpl(RideRepository rideRepository,
                           VoucherRepository voucherRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository) {
        this.rideRepository = rideRepository;
        this.voucherRepository = voucherRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public RideResponse redeemVoucher(TaxiRedeemRequest request) {
        if (request == null) {
            throw new BusinessException("Taxi redemption request cannot be null");
        }

        String voucherCode = request.getVoucherCode() != null ? request.getVoucherCode().trim() : "";
        if (voucherCode.isEmpty()) {
            throw new BusinessException("Voucher code cannot be empty");
        }

        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "code", voucherCode));

        // 1. Check if already redeemed
        if (voucher.getStatus() == VoucherStatus.REDEEMED) {
            throw new VoucherAlreadyRedeemedException(
                    String.format("Voucher %s has already been redeemed.", voucherCode)
            );
        }

        // 2. Check if expired
        if (voucher.getStatus() == VoucherStatus.EXPIRED || voucher.isExpired()) {
            if (voucher.getStatus() != VoucherStatus.EXPIRED) {
                voucher.setStatus(VoucherStatus.EXPIRED);
                voucherRepository.save(voucher);
            }
            throw new BusinessException(
                    String.format("Voucher %s expired on %s and cannot be redeemed.", voucherCode, voucher.getExpiryDate())
            );
        }

        // 3. Check status is ASSIGNED
        if (voucher.getStatus() != VoucherStatus.ASSIGNED) {
            throw new InvalidVoucherStateException(
                    String.format("Voucher %s is in %s state. Only ASSIGNED vouchers can be redeemed for rides.",
                            voucherCode, voucher.getStatus())
            );
        }

        // 4. Validate assigned commuter
        User user = voucher.getAssignedTo();
        if (user == null) {
            throw new InvalidVoucherStateException(
                    String.format("Voucher %s has not been assigned to any commuter.", voucherCode)
            );
        }

        // 5. Validate fare amount
        BigDecimal fare = request.getFare() != null ? request.getFare().setScale(2) : BigDecimal.ZERO.setScale(2);
        if (fare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Fare amount must be strictly positive");
        }
        if (fare.compareTo(voucher.getInitialValue()) > 0) {
            throw new InsufficientBalanceException(String.format(
                    "Ride fare ₹%s exceeds voucher face value ₹%s", fare, voucher.getInitialValue()
            ));
        }

        // 6. Transition voucher to REDEEMED with optimistic locking check
        voucher.setStatus(VoucherStatus.REDEEMED);
        voucher.setRedeemedAt(LocalDateTime.now());

        try {
            voucher = voucherRepository.saveAndFlush(voucher);
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
            throw new VoucherAlreadyRedeemedException(
                    String.format("Voucher %s has already been redeemed by another transaction.", voucherCode)
            );
        }

        // 7. Persist verified Ride record
        Ride ride = new Ride(
                user,
                voucher,
                request.getDriverName().trim(),
                request.getVehicleNumber().trim().toUpperCase(),
                request.getPickupLocation().trim(),
                request.getDropLocation().trim(),
                fare
        );

        Ride savedRide;
        try {
            savedRide = rideRepository.saveAndFlush(ride);
        } catch (DataIntegrityViolationException ex) {
            throw new VoucherAlreadyRedeemedException(
                    String.format("A ride record already exists for voucher %s.", voucherCode)
            );
        }

        // 8. Find user wallet and append immutable audit ledger entry
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", user.getId()));

        Transaction transaction = new Transaction(
                wallet,
                voucher,
                TransactionType.DEBIT,
                fare,
                String.format("Taxi ride redemption - Voucher %s (Driver: %s, Vehicle: %s)",
                        voucherCode, request.getDriverName().trim(), request.getVehicleNumber().trim().toUpperCase())
        );
        transactionRepository.save(transaction);

        return RideResponse.fromEntity(savedRide);
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponse getRideById(Long id) {
        Ride ride = rideRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", id));
        return RideResponse.fromEntity(ride);
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponse getRideByVoucherCode(String voucherCode) {
        Ride ride = rideRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "voucherCode", voucherCode));
        return RideResponse.fromEntity(ride);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponse> getRidesByUserId(Long userId) {
        return rideRepository.findByUserIdOrderByRideTimeDesc(userId)
                .stream()
                .map(RideResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponse> getAllRides(Pageable pageable) {
        return rideRepository.findAllByOrderByRideTimeDesc(pageable)
                .map(RideResponse::fromEntity);
    }
}
