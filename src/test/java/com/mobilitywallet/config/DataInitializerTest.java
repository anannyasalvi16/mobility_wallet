package com.mobilitywallet.config;

import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DataInitializerTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Test
    @DisplayName("Running DataInitializer twice creates no duplicates (idempotency)")
    void runningTwiceCreatesNoDuplicates() {
        long initialUsers = userRepository.count();
        long initialRoutes = routeRepository.count();
        long initialTrips = tripRepository.count();

        // Run second time manually
        dataInitializer.run();

        assertThat(userRepository.count()).isEqualTo(initialUsers);
        assertThat(routeRepository.count()).isEqualTo(initialRoutes);
        assertThat(tripRepository.count()).isEqualTo(initialTrips);
    }

    @Test
    @DisplayName("Exactly one admin and one user exist after seeding")
    void exactlyOneAdminAndOneUserSeeded() {
        List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
        List<User> users = userRepository.findByRole(Role.ROLE_USER);

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getEmail()).isEqualTo("admin@mobility.com");
        assertThat(admins.get(0).getWallet()).isNotNull();

        assertThat(users.stream().filter(u -> "user@mobility.com".equals(u.getEmail())).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Every seeded trip has a future departure time")
    void everySeededTripHasFutureDepartureTime() {
        LocalDateTime now = LocalDateTime.now();
        List<Trip> allTrips = tripRepository.findAll();

        assertThat(allTrips).isNotEmpty();
        for (Trip trip : allTrips) {
            assertThat(trip.getDepartureTime()).isAfter(now);
        }
    }

    @Test
    @DisplayName("Each seeded trip has availableSeats equal to totalSeats")
    void eachSeededTripHasAvailableSeatsEqualToTotalSeats() {
        List<Trip> allTrips = tripRepository.findAll();

        assertThat(allTrips).isNotEmpty();
        for (Trip trip : allTrips) {
            assertThat(trip.getAvailableSeats()).isEqualTo(trip.getTotalSeats());
            assertThat(trip.getAvailableSeats()).isGreaterThan(0);
        }
    }
}
