package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Ride;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.voucher WHERE r.voucher.code = :voucherCode")
    Optional<Ride> findByVoucherCode(@Param("voucherCode") String voucherCode);

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.voucher WHERE r.user.id = :userId ORDER BY r.rideTime DESC")
    List<Ride> findByUserIdOrderByRideTimeDesc(@Param("userId") Long userId);

    @Query(value = "SELECT r FROM Ride r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.voucher ORDER BY r.rideTime DESC",
           countQuery = "SELECT count(r) FROM Ride r")
    Page<Ride> findAllByOrderByRideTimeDesc(Pageable pageable);

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.voucher WHERE r.id = :id")
    Optional<Ride> findByIdWithDetails(@Param("id") Long id);

    boolean existsByVoucherId(Long voucherId);
}

