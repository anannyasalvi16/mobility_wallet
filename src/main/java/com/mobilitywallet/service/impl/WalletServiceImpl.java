package com.mobilitywallet.service.impl;

import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InsufficientBalanceException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.WalletRepository;
import com.mobilitywallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return getWalletByUserId(userId).getBalance();
    }

    @Override
    @Transactional
    public Wallet credit(Long userId, BigDecimal amount, String description, Voucher voucher) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Credit amount must be strictly positive");
        }

        Wallet wallet = getWalletByUserId(userId);
        BigDecimal newBalance = wallet.getBalance().add(amount).setScale(2);
        wallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(wallet);

        // Append immutable credit transaction to audit ledger
        Transaction transaction = new Transaction(
                savedWallet,
                voucher,
                TransactionType.CREDIT,
                amount.setScale(2),
                description
        );
        transactionRepository.save(transaction);

        return savedWallet;
    }

    @Override
    @Transactional
    public Wallet debit(Long userId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Debit amount must be strictly positive");
        }

        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Insufficient wallet balance. Available: ₹%s, Required: ₹%s",
                    wallet.getBalance(), amount.setScale(2)
            ));
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount).setScale(2);
        wallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(wallet);

        // Append immutable debit transaction to audit ledger
        Transaction transaction = new Transaction(
                savedWallet,
                null,
                TransactionType.DEBIT,
                amount.setScale(2),
                description
        );
        transactionRepository.save(transaction);

        return savedWallet;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
