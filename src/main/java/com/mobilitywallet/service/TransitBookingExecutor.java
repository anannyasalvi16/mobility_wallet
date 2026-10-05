package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Ticket;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InsufficientBalanceException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.exception.SeatOverbookedException;
import com.mobilitywallet.exception.VoucherAlreadyRedeemedException;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Isolated transactional executor for seat booking.
 * Uses Propagation.REQUIRES_NEW so that optimistic lock failures can be retried
 * cleanly in a fresh transaction without rolling back parent context.
 */
@Service
public class TransitBookingExecutor {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionRepository transactionRepository;
    private final QrCodeService qrCodeService;

    public TransitBookingExecutor(TripRepository tripRepository,
                                  BookingRepository bookingRepository,
                                  UserRepository userRepository,
                                  WalletRepository walletRepository,
                                  VoucherRepository voucherRepository,
                                  TransactionRepository transactionRepository,
                                  QrCodeService qrCodeService) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.voucherRepository = voucherRepository;
        this.transactionRepository = transactionRepository;
        this.qrCodeService = qrCodeService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookingSummaryResponse executeBookingTransaction(Long tripId, Long userId, BookingRequest request) {
        Trip trip = tripRepository.findByIdWithRoute(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));

        // 1. Verify trip departure has not elapsed
        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot book seats on a trip that has already departed.");
        }

        // 2. Validate seat availability
        int requestedSeats = request.getSeatCount();
        if (trip.getAvailableSeats() < requestedSeats) {
            throw new SeatOverbookedException(String.format(
                    "Requested %d seats, but only %d seats are available on this trip.",
                    requestedSeats, trip.getAvailableSeats()
            ));
        }

        // 3. Decrement available seats and save with @Version optimistic lock check
        trip.setAvailableSeats(trip.getAvailableSeats() - requestedSeats);
        trip = tripRepository.saveAndFlush(trip);

        // 4. Calculate total fare
        BigDecimal totalFare = trip.getFare().multiply(BigDecimal.valueOf(requestedSeats)).setScale(2);

        // 5. Look up commuter and digital wallet
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));

        // 6. Settle payment (Wallet vs Voucher vs Split Payment)
        BigDecimal voucherAmountUsed = BigDecimal.ZERO.setScale(2);
        BigDecimal walletAmountUsed = BigDecimal.ZERO.setScale(2);
        String bookingReference = "BKG-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        if (request.getPaymentMethod() == PaymentMethod.WALLET) {
            if (wallet.getBalance().compareTo(totalFare) < 0) {
                throw new InsufficientBalanceException(String.format(
                        "Insufficient wallet balance. Required: ₹%s, Available: ₹%s",
                        totalFare, wallet.getBalance()
                ));
            }

            wallet.setBalance(wallet.getBalance().subtract(totalFare).setScale(2));
            walletRepository.saveAndFlush(wallet);
            walletAmountUsed = totalFare;

            Transaction tx = new Transaction(
                    wallet,
                    null,
                    TransactionType.DEBIT,
                    totalFare,
                    String.format("Transit booking %s (%d seats on %s -> %s)",
                            bookingReference, requestedSeats, trip.getRoute().getOrigin(), trip.getRoute().getDestination())
            );
            transactionRepository.save(tx);

        } else if (request.getPaymentMethod() == PaymentMethod.VOUCHER) {
            String voucherCode = request.getVoucherCode() != null ? request.getVoucherCode().trim() : "";
            if (voucherCode.isEmpty()) {
                throw new BusinessException("Voucher code is required when payment method is VOUCHER");
            }

            Voucher voucher = voucherRepository.findByCode(voucherCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher", "code", voucherCode));

            if (voucher.getStatus() == VoucherStatus.REDEEMED) {
                throw new VoucherAlreadyRedeemedException("Voucher " + voucherCode + " has already been redeemed.");
            }
            if (voucher.getStatus() == VoucherStatus.EXPIRED || voucher.isExpired()) {
                throw new BusinessException("Voucher " + voucherCode + " has expired.");
            }
            if (voucher.getStatus() != VoucherStatus.ASSIGNED || voucher.getAssignedTo() == null || !voucher.getAssignedTo().getId().equals(userId)) {
                throw new BusinessException("Voucher " + voucherCode + " is not assigned to your account.");
            }

            if (voucher.getInitialValue().compareTo(totalFare) >= 0) {
                // Full fare covered by voucher
                voucherAmountUsed = totalFare;
                voucher.setStatus(VoucherStatus.REDEEMED);
                voucher.setRedeemedAt(LocalDateTime.now());
                voucherRepository.saveAndFlush(voucher);

                Transaction tx = new Transaction(
                        wallet,
                        voucher,
                        TransactionType.DEBIT,
                        totalFare,
                        String.format("Transit booking %s covered by voucher %s", bookingReference, voucherCode)
                );
                transactionRepository.save(tx);

            } else {
                // Split payment: voucher face value + remaining wallet balance
                voucherAmountUsed = voucher.getInitialValue();
                BigDecimal remainder = totalFare.subtract(voucherAmountUsed).setScale(2);

                if (wallet.getBalance().compareTo(remainder) < 0) {
                    throw new InsufficientBalanceException(String.format(
                            "Insufficient wallet balance for remainder. Required: ₹%s, Available: ₹%s (Voucher covered: ₹%s of ₹%s)",
                            remainder, wallet.getBalance(), voucherAmountUsed, totalFare
                    ));
                }

                wallet.setBalance(wallet.getBalance().subtract(remainder).setScale(2));
                walletRepository.saveAndFlush(wallet);
                walletAmountUsed = remainder;

                voucher.setStatus(VoucherStatus.REDEEMED);
                voucher.setRedeemedAt(LocalDateTime.now());
                voucherRepository.saveAndFlush(voucher);

                Transaction txVoucher = new Transaction(
                        wallet,
                        voucher,
                        TransactionType.DEBIT,
                        voucherAmountUsed,
                        String.format("Transit booking %s - Voucher portion (%s)", bookingReference, voucherCode)
                );
                transactionRepository.save(txVoucher);

                Transaction txWallet = new Transaction(
                        wallet,
                        null,
                        TransactionType.DEBIT,
                        walletAmountUsed,
                        String.format("Transit booking %s - Wallet balance portion", bookingReference)
                );
                transactionRepository.save(txWallet);
            }
        }

        // 7. Persist Booking entity
        Booking booking = new Booking(bookingReference, user, trip, requestedSeats, totalFare, request.getPaymentMethod());

        // 8. Generate individual tickets with QR codes
        for (int i = 1; i <= requestedSeats; i++) {
            int seatIdx = trip.getTotalSeats() - trip.getAvailableSeats() - requestedSeats + i;
            String seatNumber = "S" + seatIdx;
            String ticketNumber = "TKT-" + bookingReference + "-" + i;
            String qrPayload = "TKT:" + ticketNumber + "|TRIP:" + trip.getId() + "|SEAT:" + seatNumber;
            String qrDataUri = qrCodeService.generateQrCodeDataUri(qrPayload, 250, 250);

            Ticket ticket = new Ticket(booking, seatNumber, ticketNumber, qrDataUri);
            booking.addTicket(ticket);
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        return BookingSummaryResponse.fromEntity(savedBooking, voucherAmountUsed, walletAmountUsed);
    }
}
