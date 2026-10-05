package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.user.id = :userId")
    Optional<Wallet> findByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Wallet w JOIN FETCH w.user WHERE LOWER(w.user.email) = LOWER(:email)")
    Optional<Wallet> findByUserEmail(@Param("email") String email);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w")
    java.math.BigDecimal sumAllBalances();
}
