package com.mobilitywallet.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.request.RouteCreateRequest;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TicketRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransitApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private com.mobilitywallet.repository.TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User commuter;
    private Route route;
    private Trip trip;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin@mobility.com").orElseGet(() -> {
            User a = new User("System Administrator", "admin@mobility.com", passwordEncoder.encode("admin123"), "9876500000", Role.ROLE_ADMIN);
            Wallet w = new Wallet(a, BigDecimal.ZERO);
            a.setWallet(w);
            return userRepository.save(a);
        });

        commuter = userRepository.findByEmail("user@mobility.com").orElseGet(() -> {
            User c = new User("John Doe", "user@mobility.com", passwordEncoder.encode("user123"), "9876511111", Role.ROLE_USER);
            Wallet w = new Wallet(c, new BigDecimal("750.00"));
            c.setWallet(w);
            return userRepository.save(c);
        });

        // Ensure commuter has at least 500 balance
        Wallet w = walletRepository.findByUserId(commuter.getId()).orElseThrow();
        if (w.getBalance().compareTo(new BigDecimal("500.00")) < 0) {
            w.setBalance(new BigDecimal("750.00"));
            walletRepository.saveAndFlush(w);
        }

        route = routeRepository.saveAndFlush(new Route("Station Alpha", "Station Beta", 18.0, TransitMode.METRO));

        trip = tripRepository.saveAndFlush(new Trip(
                route,
                LocalDateTime.now().plusDays(4).withHour(9).withMinute(0),
                LocalDateTime.now().plusDays(4).withHour(9).withMinute(40),
                40,
                new BigDecimal("35.00")
        ));
    }

    @AfterEach
    void tearDown() {
        if (trip != null && trip.getId() != null) {
            List<com.mobilitywallet.entity.Booking> bookings = bookingRepository.findByTripId(trip.getId());
            for (com.mobilitywallet.entity.Booking b : bookings) {
                ticketRepository.deleteAll(ticketRepository.findByBookingId(b.getId()));
                bookingRepository.delete(b);
            }
            tripRepository.findById(trip.getId()).ifPresent(tripRepository::delete);
        }
        if (route != null && route.getId() != null) {
            routeRepository.findById(route.getId()).ifPresent(routeRepository::delete);
        }
        routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("East Hub", "West Terminal")
                .ifPresent(r -> {
                    tripRepository.findAll().stream()
                            .filter(t -> t.getRoute() != null && t.getRoute().getId().equals(r.getId()))
                            .forEach(t -> {
                                bookingRepository.findByTripId(t.getId()).forEach(b -> {
                                    ticketRepository.deleteAll(ticketRepository.findByBookingId(b.getId()));
                                    bookingRepository.delete(b);
                                });
                                tripRepository.delete(t);
                            });
                    routeRepository.delete(r);
                });

        userRepository.findByEmail("user@mobility.com").ifPresent(u -> {
            transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(u.getId()).stream()
                    .filter(tx -> tx.getDescription() != null && tx.getDescription().contains("Station Alpha"))
                    .forEach(transactionRepository::delete);
            walletRepository.findByUserId(u.getId()).ifPresent(wallet -> {
                wallet.setBalance(new BigDecimal("750.00"));
                walletRepository.saveAndFlush(wallet);
            });
        });
    }

    @Test
    @DisplayName("GET /api/transit/routes returns 200 OK with active routes")
    void testGetActiveRoutes() throws Exception {
        mockMvc.perform(get("/api/transit/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/transit/routes requires ROLE_ADMIN: 201 for admin, 403 for user")
    void testCreateRouteAccessControl() throws Exception {
        RouteCreateRequest request = new RouteCreateRequest("East Hub", "West Terminal", TransitMode.BUS, 15.0);

        // Commuter gets 403 Forbidden
        mockMvc.perform(post("/api/transit/routes")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Admin gets 201 Created
        mockMvc.perform(post("/api/transit/routes")
                .with(httpBasic("admin@mobility.com", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.origin").value("East Hub"));
    }

    @Test
    @DisplayName("GET /api/transit/trips/search returns 200 OK with matching scheduled trips")
    void testSearchTrips() throws Exception {
        mockMvc.perform(get("/api/transit/trips/search")
                .param("origin", "Station Alpha")
                .param("destination", "Station Beta")
                .param("date", LocalDate.now().plusDays(4).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].origin").value("Station Alpha"));
    }

    @Test
    @DisplayName("POST /api/transit/bookings books seats with wallet and issues tickets")
    void testBookSeatsEndpoint() throws Exception {
        BookingRequest request = new BookingRequest(trip.getId(), 2, PaymentMethod.WALLET, null);

        mockMvc.perform(post("/api/transit/bookings")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seatCount").value(2))
                .andExpect(jsonPath("$.data.totalFare").value(70.00))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.tickets.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/transit/bookings/{id}/cancel cancels booking and refunds wallet")
    void testCancelBookingEndpoint() throws Exception {
        BookingRequest request = new BookingRequest(trip.getId(), 1, PaymentMethod.WALLET, null);

        String responseJson = mockMvc.perform(post("/api/transit/bookings")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // Cancel
        mockMvc.perform(post("/api/transit/bookings/" + bookingId + "/cancel")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/transit/bookings/my returns list of commuter bookings")
    void testGetMyBookings() throws Exception {
        mockMvc.perform(get("/api/transit/bookings/my")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
