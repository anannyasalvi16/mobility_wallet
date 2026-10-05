package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    @Query("SELECT v FROM Voucher v LEFT JOIN FETCH v.assignedTo LEFT JOIN FETCH v.createdBy WHERE v.code = :code")
    Optional<Voucher> findByCode(@Param("code") String code);

    boolean existsByCode(String code);

    @Query("SELECT v FROM Voucher v LEFT JOIN FETCH v.assignedTo LEFT JOIN FETCH v.createdBy WHERE v.assignedTo.id = :userId ORDER BY v.expiryDate ASC")
    List<Voucher> findByAssignedToIdOrderByExpiryDateAsc(@Param("userId") Long userId);

    @Query("SELECT v FROM Voucher v LEFT JOIN FETCH v.assignedTo LEFT JOIN FETCH v.createdBy WHERE v.assignedTo.id = :userId AND v.status = :status ORDER BY v.expiryDate ASC")
    List<Voucher> findByAssignedToIdAndStatusOrderByExpiryDateAsc(@Param("userId") Long userId, @Param("status") VoucherStatus status);

    List<Voucher> findByStatus(VoucherStatus status);

    Page<Voucher> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT v FROM Voucher v LEFT JOIN FETCH v.assignedTo LEFT JOIN FETCH v.createdBy ORDER BY v.createdAt DESC",
           countQuery = "SELECT count(v) FROM Voucher v")
    Page<Voucher> findAllWithUsers(Pageable pageable);

    @Query("SELECT v FROM Voucher v LEFT JOIN FETCH v.assignedTo LEFT JOIN FETCH v.createdBy ORDER BY v.createdAt DESC")
    List<Voucher> findAllWithUsers();

    long countByStatus(VoucherStatus status);

    List<Voucher> findByStatusAndExpiryDateBefore(VoucherStatus status, LocalDate date);

    @Query("SELECT COALESCE(SUM(v.initialValue), 0) FROM Voucher v WHERE v.status = :status")
    BigDecimal sumInitialValueByStatus(@Param("status") VoucherStatus status);
}
