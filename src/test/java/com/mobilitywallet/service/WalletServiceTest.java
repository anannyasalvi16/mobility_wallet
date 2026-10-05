package com.mobilitywallet.service;

import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.exception.InsufficientBalanceException;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.WalletRepository;
import com.mobilitywallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private WalletService walletService;

    private User sampleUser;
    private Wallet sampleWallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletRepository, transactionRepository);

        sampleUser = new User("Alice", "alice@corp.com", "hash", "123456", Role.ROLE_USER);
        sampleUser.setId(10L);

        sampleWallet = new Wallet(sampleUser, new BigDecimal("500.00"));
        sampleWallet.setId(100L);
    }

    @Test
    @DisplayName("Credit wallet: Increases balance and appends CREDIT entry to Transaction ledger")
    void creditWalletSuccess() {
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(sampleWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet updated = walletService.credit(10L, new BigDecimal("250.00"), "Bonus travel credit", null);

        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(savedTx.getDescription()).isEqualTo("Bonus travel credit");
        assertThat(savedTx.getWallet()).isEqualTo(sampleWallet);
    }

    @Test
    @DisplayName("Credit wallet with zero or negative amount throws BusinessException")
    void creditNegativeAmountThrows() {
        assertThatThrownBy(() -> walletService.credit(10L, new BigDecimal("0.00"), "Zero credit", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("strictly positive");

        assertThatThrownBy(() -> walletService.credit(10L, new BigDecimal("-50.00"), "Negative credit", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    @DisplayName("Debit wallet: Decreases balance and appends DEBIT entry to Transaction ledger")
    void debitWalletSuccess() {
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(sampleWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet updated = walletService.debit(10L, new BigDecimal("150.00"), "Bus ticket booking #BKG-101");

        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("350.00"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(savedTx.getDescription()).isEqualTo("Bus ticket booking #BKG-101");
    }

    @Test
    @DisplayName("Debit wallet with insufficient balance throws InsufficientBalanceException")
    void debitInsufficientBalanceThrows() {
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(sampleWallet));

        assertThatThrownBy(() -> walletService.debit(10L, new BigDecimal("600.00"), "Expensive ticket"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    @DisplayName("Get paginated transaction history")
    void getTransactionHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx = new Transaction(sampleWallet, null, TransactionType.CREDIT, new BigDecimal("500.00"), "Initial");
        Page<Transaction> page = new PageImpl<>(List.of(tx), pageable, 1);

        when(transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(10L, pageable)).thenReturn(page);

        Page<Transaction> result = walletService.getTransactionHistory(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
