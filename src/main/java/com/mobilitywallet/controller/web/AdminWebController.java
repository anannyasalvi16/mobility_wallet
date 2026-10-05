package com.mobilitywallet.controller.web;

import com.mobilitywallet.dto.request.RouteCreateRequest;
import com.mobilitywallet.dto.request.TripCreateRequest;
import com.mobilitywallet.dto.request.VoucherBulkCreateRequest;
import com.mobilitywallet.dto.request.VoucherCreateRequest;
import com.mobilitywallet.dto.response.AdminAnalyticsDto;
import com.mobilitywallet.entity.Ride;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.service.AnalyticsService;
import com.mobilitywallet.service.TransitService;
import com.mobilitywallet.service.UserService;
import com.mobilitywallet.service.VoucherService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final TransitService transitService;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    public AdminWebController(UserService userService,
                              AnalyticsService analyticsService,
                              VoucherService voucherService,
                              VoucherRepository voucherRepository,
                              TransitService transitService,
                              RouteRepository routeRepository,
                              TripRepository tripRepository,
                              RideRepository rideRepository,
                              UserRepository userRepository) {
        this.userService = userService;
        this.analyticsService = analyticsService;
        this.voucherService = voucherService;
        this.voucherRepository = voucherRepository;
        this.transitService = transitService;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false, defaultValue = "analytics") String tab,
                            @RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            Model model,
                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User admin = userService.findByEmail(authentication.getName());
        AdminAnalyticsDto analytics = analyticsService.getAdminAnalytics();
        List<Voucher> vouchers = voucherRepository.findAllWithUsers(PageRequest.of(0, 100)).getContent();
        List<User> commuters = userRepository.findByRole(Role.ROLE_USER);
        List<Route> routes = routeRepository.findAll();
        List<Trip> trips = tripRepository.findAllWithRoute(PageRequest.of(0, 50)).getContent();
        List<Ride> rides = rideRepository.findAllByOrderByRideTimeDesc(PageRequest.of(0, 50)).getContent();
        List<Transaction> recentTransactions = analyticsService.getRecentTransactions(5);

        model.addAttribute("user", admin);
        model.addAttribute("role", admin.getRole().name());
        model.addAttribute("analytics", analytics);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("commuters", commuters);
        model.addAttribute("routes", routes);
        model.addAttribute("trips", trips);
        model.addAttribute("rides", rides);
        model.addAttribute("activeTab", tab);
        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error);

        return "admin/dashboard";
    }

    @PostMapping("/vouchers/create")
    public String createVoucher(@RequestParam String code,
                                @RequestParam BigDecimal initialValue,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                @RequestParam(required = false) Long assignToUserId,
                                Authentication authentication) {
        try {
            User admin = userService.findByEmail(authentication.getName());
            VoucherCreateRequest req = new VoucherCreateRequest();
            req.setInitialValue(initialValue);
            req.setExpiryDate(expiryDate);

            Voucher voucher = voucherService.createVoucher(req, admin);
            if (assignToUserId != null) {
                voucherService.assignVoucher(voucher.getId(), assignToUserId);
            }

            return "redirect:/admin/dashboard?tab=vouchers&success=" + encode("Voucher " + voucher.getCode() + " created successfully!");
        } catch (Exception ex) {
            return "redirect:/admin/dashboard?tab=vouchers&error=" + encode(ex.getMessage());
        }
    }

    @PostMapping("/vouchers/bulk")
    public String createBulkVouchers(@RequestParam Integer count,
                                     @RequestParam BigDecimal initialValue,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                     Authentication authentication) {
        try {
            User admin = userService.findByEmail(authentication.getName());
            VoucherBulkCreateRequest req = new VoucherBulkCreateRequest();
            req.setCount(count);
            req.setInitialValue(initialValue);
            req.setExpiryDate(expiryDate);

            List<Voucher> created = voucherService.createBulkVouchers(req, admin);
            return "redirect:/admin/dashboard?tab=vouchers&success=" + encode("Batch of " + created.size() + " vouchers generated successfully!");
        } catch (Exception ex) {
            return "redirect:/admin/dashboard?tab=vouchers&error=" + encode(ex.getMessage());
        }
    }

    @PostMapping("/vouchers/assign")
    public String assignVoucher(@RequestParam Long voucherId,
                                @RequestParam Long userId) {
        try {
            voucherService.assignVoucher(voucherId, userId);
            return "redirect:/admin/dashboard?tab=vouchers&success=" + encode("Voucher assigned successfully!");
        } catch (Exception ex) {
            return "redirect:/admin/dashboard?tab=vouchers&error=" + encode(ex.getMessage());
        }
    }

    @PostMapping("/routes/create")
    public String createRoute(@RequestParam String origin,
                              @RequestParam String destination,
                              @RequestParam Double distanceKm,
                              @RequestParam TransitMode mode) {
        try {
            RouteCreateRequest req = new RouteCreateRequest();
            req.setOrigin(origin.trim());
            req.setDestination(destination.trim());
            req.setDistanceKm(distanceKm);
            req.setMode(mode);

            transitService.createRoute(req);
            return "redirect:/admin/dashboard?tab=transit&success=" + encode("Route created successfully!");
        } catch (Exception ex) {
            return "redirect:/admin/dashboard?tab=transit&error=" + encode(ex.getMessage());
        }
    }

    @PostMapping("/trips/create")
    public String createTrip(@RequestParam Long routeId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTime,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime,
                             @RequestParam Integer totalSeats,
                             @RequestParam BigDecimal fare) {
        try {
            TripCreateRequest req = new TripCreateRequest();
            req.setRouteId(routeId);
            req.setDepartureTime(departureTime);
            req.setArrivalTime(arrivalTime);
            req.setTotalSeats(totalSeats);
            req.setFare(fare);

            transitService.createTrip(req);
            return "redirect:/admin/dashboard?tab=transit&success=" + encode("Trip scheduled successfully!");
        } catch (Exception ex) {
            return "redirect:/admin/dashboard?tab=transit&error=" + encode(ex.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
