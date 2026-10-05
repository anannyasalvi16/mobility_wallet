package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT DISTINCT b FROM Booking b JOIN FETCH b.trip t JOIN FETCH t.route r JOIN FETCH b.user u LEFT JOIN FETCH b.tickets WHERE b.bookingReference = :bookingReference")
    Optional<Booking> findByBookingReference(@Param("bookingReference") String bookingReference);

    @Query("SELECT DISTINCT b FROM Booking b JOIN FETCH b.trip t JOIN FETCH t.route r JOIN FETCH b.user u LEFT JOIN FETCH b.tickets WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT b FROM Booking b JOIN FETCH b.trip t JOIN FETCH t.route r JOIN FETCH b.user u LEFT JOIN FETCH b.tickets WHERE b.user.id = :userId ORDER BY b.bookingDate DESC")
    List<Booking> findByUserIdOrderByBookingDateDesc(@Param("userId") Long userId);

    Page<Booking> findByUserIdOrderByBookingDateDesc(Long userId, Pageable pageable);

    Page<Booking> findAllByOrderByBookingDateDesc(Pageable pageable);

    List<Booking> findByTripId(Long tripId);
}
