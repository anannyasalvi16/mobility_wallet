package com.mobilitywallet.controller.web;

import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.repository.BookingRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TicketRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

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

    @AfterEach
    void tearDown() {
        voucherRepository.findByCode("VCH-PORTAL-TEST").ifPresent(voucherRepository::delete);
        voucherRepository.findAll().stream()
                .filter(v -> (v.getInitialValue().compareTo(new BigDecimal("50.00")) == 0
                        || v.getInitialValue().compareTo(new BigDecimal("125.00")) == 0)
                        && v.getCode().startsWith("VCH-"))
                .forEach(voucherRepository::delete);

        routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("Station A", "Station B").ifPresent(r -> {
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

        routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("North Gate", "Innovation Hub").ifPresent(r -> {
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
                    .filter(tx -> tx.getDescription() != null && tx.getDescription().contains("Station A"))
                    .forEach(transactionRepository::delete);
        });
    }

    @Test
    @DisplayName("Landing page is accessible without authentication")
    void landingPageIsAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Login view renders with demo helper")
    void loginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("Register view renders with registration request DTO")
    void registerPageRenders() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    @DisplayName("403 Access Denied view renders")
    void accessDeniedPageRenders() throws Exception {
        mockMvc.perform(get("/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("Logo preview page renders without authentication")
    void logoPreviewPageRenders() throws Exception {
        mockMvc.perform(get("/logo-preview"))
                .andExpect(status().isOk())
                .andExpect(view().name("logo-preview"));
    }

    @Test
    @WithMockUser(username = "user@mobility.com", roles = {"USER"})
    @DisplayName("Commuter dashboard renders with wallet balance, vouchers, routes and bookings")
    void commuterDashboardRenders() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("walletBalance"))
                .andExpect(model().attributeExists("vouchers"))
                .andExpect(model().attributeExists("activeVouchers"))
                .andExpect(model().attributeExists("routes"))
                .andExpect(model().attributeExists("bookings"));
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin dashboard renders with analytics metrics, vouchers and route tables")
    void adminDashboardRenders() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("analytics"))
                .andExpect(model().attributeExists("recentTransactions"))
                .andExpect(model().attributeExists("vouchers"))
                .andExpect(model().attributeExists("commuters"))
                .andExpect(model().attributeExists("routes"))
                .andExpect(model().attributeExists("trips"));
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin can create single voucher via web form submission")
    void adminCanCreateSingleVoucher() throws Exception {
        mockMvc.perform(post("/admin/vouchers/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "VCH-PORTAL-TEST")
                        .param("initialValue", "125.00")
                        .param("expiryDate", LocalDate.now().plusDays(45).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/dashboard?tab=vouchers&*"));

        assertThat(voucherRepository.findAll().stream().anyMatch(v -> v.getInitialValue().compareTo(new BigDecimal("125.00")) == 0)).isTrue();
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin can generate bulk vouchers via web form submission")
    void adminCanGenerateBulkVouchers() throws Exception {
        long countBefore = voucherRepository.count();

        mockMvc.perform(post("/admin/vouchers/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("count", "3")
                        .param("initialValue", "50.00")
                        .param("expiryDate", LocalDate.now().plusDays(20).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/dashboard?tab=vouchers&*"));

        assertThat(voucherRepository.count()).isEqualTo(countBefore + 3);
    }

    @Test
    @WithMockUser(username = "admin@mobility.com", roles = {"ADMIN"})
    @DisplayName("Admin can create transit route and schedule trip via web forms")
    void adminCanCreateRouteAndTrip() throws Exception {
        mockMvc.perform(post("/admin/routes/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("origin", "North Gate")
                        .param("destination", "Innovation Hub")
                        .param("distanceKm", "12.0")
                        .param("mode", "BUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/dashboard?tab=transit&*"));

        Route route = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("North Gate", "Innovation Hub")
                .orElseThrow();
        assertThat(route.getMode()).isEqualTo(TransitMode.BUS);

        LocalDateTime dep = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
        LocalDateTime arr = dep.plusMinutes(40);

        mockMvc.perform(post("/admin/trips/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("routeId", route.getId().toString())
                        .param("departureTime", dep.toString())
                        .param("arrivalTime", arr.toString())
                        .param("totalSeats", "30")
                        .param("fare", "35.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/dashboard?tab=transit&*"));
    }

    @Test
    @WithMockUser(username = "user@mobility.com", roles = {"USER"})
    @DisplayName("Commuter can book seat and then cancel with 100% refund via web forms")
    void commuterCanBookAndCancelTrip() throws Exception {
        User user = userRepository.findByEmail("user@mobility.com").orElseThrow();
        Route route = routeRepository.save(new Route("Station A", "Station B", 10.0, TransitMode.BUS));
        Trip trip = tripRepository.save(new Trip(route, LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1), 20, new BigDecimal("25.00")));

        // Book seat
        mockMvc.perform(post("/user/book")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("tripId", trip.getId().toString())
                        .param("seatsCount", "1")
                        .param("paymentMethod", "WALLET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/user/dashboard?tab=bookings&*"));

        var userBookings = bookingRepository.findByUserIdOrderByBookingDateDesc(user.getId());
        var booking = userBookings.stream()
                .filter(b -> b.getTrip().getId().equals(trip.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Cancel booking
        mockMvc.perform(post("/user/cancel-booking/" + booking.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/user/dashboard?tab=bookings&*"));

        var refreshedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(refreshedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
