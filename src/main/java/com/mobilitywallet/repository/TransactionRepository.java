package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Transaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.voucher LEFT JOIN FETCH t.wallet WHERE t.wallet.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByWalletUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'DEBIT' AND t.createdAt >= :since")
    BigDecimal sumDebitAmountSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'DEBIT' AND t.createdAt >= :start AND t.createdAt < :end")
    BigDecimal sumDebitAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.voucher LEFT JOIN FETCH t.wallet WHERE t.voucher.code = :voucherCode")
    List<Transaction> findByVoucherCode(@Param("voucherCode") String voucherCode);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.voucher LEFT JOIN FETCH t.wallet w LEFT JOIN FETCH w.user ORDER BY t.createdAt DESC")
    List<Transaction> findTopWithDetailsOrderByCreatedAtDesc(Pageable pageable);
}
