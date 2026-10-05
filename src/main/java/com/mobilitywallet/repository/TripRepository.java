package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("SELECT t FROM Trip t JOIN FETCH t.route r " +
           "WHERE LOWER(r.origin) = LOWER(:origin) " +
           "AND LOWER(r.destination) = LOWER(:destination) " +
           "AND t.departureTime >= :startTime AND t.departureTime <= :endTime " +
           "AND t.availableSeats > 0 " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchAvailableTrips(@Param("origin") String origin,
                                   @Param("destination") String destination,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route r WHERE t.id = :id")
    Optional<Trip> findByIdWithRoute(@Param("id") Long id);

    @Query(value = "SELECT t FROM Trip t JOIN FETCH t.route r ORDER BY t.departureTime DESC",
           countQuery = "SELECT count(t) FROM Trip t")
    Page<Trip> findAllWithRoute(Pageable pageable);

    List<Trip> findByDepartureTimeAfterOrderByDepartureTimeAsc(LocalDateTime after);

    Page<Trip> findAllByOrderByDepartureTimeDesc(Pageable pageable);
}
