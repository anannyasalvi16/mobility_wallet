package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b JOIN FETCH b.trip tr JOIN FETCH tr.route r WHERE t.ticketNumber = :ticketNumber")
    Optional<Ticket> findByTicketNumber(@Param("ticketNumber") String ticketNumber);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b JOIN FETCH b.trip tr JOIN FETCH tr.route r WHERE t.id = :id")
    Optional<Ticket> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.booking b WHERE b.id = :bookingId")
    List<Ticket> findByBookingId(@Param("bookingId") Long bookingId);

    List<Ticket> findByBookingUserIdOrderByBookingBookingDateDesc(Long userId);
}
