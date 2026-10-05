package com.mobilitywallet.service;

import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {

    Wallet getWalletByUserId(Long userId);

    BigDecimal getBalance(Long userId);

    Wallet credit(Long userId, BigDecimal amount, String description, Voucher voucher);

    Wallet debit(Long userId, BigDecimal amount, String description);

    Page<Transaction> getTransactionHistory(Long userId, Pageable pageable);
}
