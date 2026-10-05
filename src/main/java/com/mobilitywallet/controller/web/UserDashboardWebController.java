package com.mobilitywallet.controller.web;

import com.mobilitywallet.dto.request.BookingRequest;
import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.dto.response.BookingSummaryResponse;
import com.mobilitywallet.dto.response.RideResponse;
import com.mobilitywallet.dto.response.TripSearchResponse;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.service.QrCodeService;
import com.mobilitywallet.service.RideService;
import com.mobilitywallet.service.TransitService;
import com.mobilitywallet.service.UserService;
import com.mobilitywallet.service.VoucherService;
import com.mobilitywallet.service.WalletService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserDashboardWebController {

    private final UserService userService;
    private final WalletService walletService;
    private final VoucherService voucherService;
    private final TransitService transitService;
    private final RideService rideService;
    private final QrCodeService qrCodeService;
    private final TransactionRepository transactionRepository;

    public UserDashboardWebController(UserService userService,
                                      WalletService walletService,
                                      VoucherService voucherService,
                                      TransitService transitService,
                                      RideService rideService,
                                      QrCodeService qrCodeService,
                                      TransactionRepository transactionRepository) {
        this.userService = userService;
        this.walletService = walletService;
        this.voucherService = voucherService;
        this.transitService = transitService;
        this.rideService = rideService;
        this.qrCodeService = qrCodeService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false, defaultValue = "overview") String tab,
                            @RequestParam(required = false) String origin,
                            @RequestParam(required = false) String destination,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
                            @RequestParam(required = false) String success,
                            @RequestParam(required = false) String error,
                            Model model,
                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(authentication.getName());
        BigDecimal balance = walletService.getBalance(user.getId());
        List<Voucher> vouchers = voucherService.getVouchersForUser(user.getId());
        List<Voucher> activeVouchers = voucherService.getActiveVouchersForUser(user.getId());

        // Precompute QR codes for active vouchers
        Map<String, String> voucherQrMap = new HashMap<>();
        for (Voucher v : activeVouchers) {
            voucherQrMap.put(v.getCode(), qrCodeService.generateQrCodeDataUri(v.getCode(), 250, 250));
        }

        List<Route> routes = transitService.getAllActiveRoutes();

        // Search trips if requested
        List<TripSearchResponse> searchResults = null;
        if (origin != null && !origin.isBlank() && destination != null && !destination.isBlank() && travelDate != null) {
            searchResults = transitService.searchTrips(origin.trim(), destination.trim(), travelDate);
            tab = "transit";
        }

        List<BookingSummaryResponse> bookings = transitService.getCommuterBookings(user.getId());
        List<RideResponse> rides = rideService.getRidesByUserId(user.getId());
        List<Transaction> transactions = transactionRepository.findByWalletUserIdWithDetailsOrderByCreatedAtDesc(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("walletBalance", balance);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("activeVouchers", activeVouchers);
        model.addAttribute("voucherQrMap", voucherQrMap);
        model.addAttribute("routes", routes);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchOrigin", origin);
        model.addAttribute("searchDestination", destination);
        model.addAttribute("searchTravelDate", travelDate != null ? travelDate.toString() : LocalDate.now().toString());
        model.addAttribute("bookings", bookings);
        model.addAttribute("rides", rides);
        model.addAttribute("transactions", transactions);
        model.addAttribute("activeTab", tab);
        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error);

        return "user/dashboard";
    }

    @PostMapping("/book")
    public String bookSeats(@RequestParam Long tripId,
                            @RequestParam(defaultValue = "1") Integer seatsCount,
                            @RequestParam(defaultValue = "WALLET") PaymentMethod paymentMethod,
                            @RequestParam(required = false) String voucherCode,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(authentication.getName());
            BookingRequest request = new BookingRequest();
            request.setSeatCount(seatsCount);
            request.setPaymentMethod(paymentMethod);
            if (voucherCode != null && !voucherCode.isBlank()) {
                request.setVoucherCode(voucherCode.trim());
            }

            BookingSummaryResponse booking = transitService.bookSeatsWithRetry(tripId, user.getId(), request);
            return "redirect:/user/dashboard?tab=bookings&success=" + encode("Booking confirmed! Booking Ref: " + booking.getBookingReference());
        } catch (Exception ex) {
            return "redirect:/user/dashboard?tab=transit&error=" + encode(ex.getMessage());
        }
    }

    @PostMapping("/cancel-booking/{id}")
    public String cancelBooking(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(authentication.getName());
            transitService.cancelBooking(id, user.getId());
            return "redirect:/user/dashboard?tab=bookings&success=" + encode("Booking cancelled successfully! 100% fare refunded to your wallet.");
        } catch (Exception ex) {
            return "redirect:/user/dashboard?tab=bookings&error=" + encode(ex.getMessage());
        }
    }

    @PostMapping("/redeem-taxi")
    public String redeemTaxi(@RequestParam String voucherCode,
                             @RequestParam String driverName,
                             @RequestParam String vehicleNumber,
                             @RequestParam String pickupLocation,
                             @RequestParam String dropLocation,
                             @RequestParam BigDecimal fare,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            TaxiRedeemRequest request = new TaxiRedeemRequest();
            request.setVoucherCode(voucherCode.trim());
            request.setDriverName(driverName.trim());
            request.setVehicleNumber(vehicleNumber.trim());
            request.setPickupLocation(pickupLocation.trim());
            request.setDropLocation(dropLocation.trim());
            request.setFare(fare);

            rideService.redeemVoucher(request);
            return "redirect:/user/dashboard?tab=vouchers&success=" + encode("Taxi ride redeemed successfully with voucher " + voucherCode + "!");
        } catch (Exception ex) {
            return "redirect:/user/dashboard?tab=vouchers&error=" + encode(ex.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
