package com.mobilitywallet.repository;

import com.mobilitywallet.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByActiveTrue();

    Optional<Route> findByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);
}
