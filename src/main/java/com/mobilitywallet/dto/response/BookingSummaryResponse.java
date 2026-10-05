package com.mobilitywallet.dto.response;

import com.mobilitywallet.entity.Booking;
import com.mobilitywallet.entity.BookingStatus;
import com.mobilitywallet.entity.PaymentMethod;
import com.mobilitywallet.entity.TransitMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Summary of a confirmed or cancelled transit booking")
public class BookingSummaryResponse {

    @Schema(description = "Booking ID", example = "1")
    private Long id;

    @Schema(description = "Unique booking reference code", example = "BKG-2026-X89J2")
    private String bookingReference;

    @Schema(description = "Commuter user ID", example = "2")
    private Long userId;

    @Schema(description = "Commuter full name", example = "John Doe")
    private String userName;

    @Schema(description = "Commuter email address", example = "user@mobility.com")
    private String userEmail;

    @Schema(description = "Trip ID", example = "1")
    private Long tripId;

    @Schema(description = "Starting point", example = "Campus Main Gate")
    private String origin;

    @Schema(description = "Destination", example = "Tech Park Station")
    private String destination;

    @Schema(description = "Transit mode (BUS, METRO)", example = "BUS")
    private TransitMode transitMode;

    @Schema(description = "Departure timestamp", example = "2026-10-04T09:00:00")
    private LocalDateTime departureTime;

    @Schema(description = "Estimated arrival timestamp", example = "2026-10-04T09:45:00")
    private LocalDateTime arrivalTime;

    @Schema(description = "Number of reserved seats", example = "2")
    private Integer seatCount;

    @Schema(description = "Total booking fare", example = "70.00")
    private BigDecimal totalFare;

    @Schema(description = "Payment method selected", example = "WALLET")
    private PaymentMethod paymentMethod;

    @Schema(description = "Portion settled via voucher", example = "50.00")
    private BigDecimal voucherAmountUsed;

    @Schema(description = "Portion settled via digital wallet balance", example = "20.00")
    private BigDecimal walletAmountUsed;

    @Schema(description = "Booking status (CONFIRMED, CANCELLED)", example = "CONFIRMED")
    private BookingStatus status;

    @Schema(description = "Booking creation timestamp", example = "2026-10-03T19:00:00")
    private LocalDateTime bookingDate;

    @Schema(description = "List of issued seat tickets with QR codes")
    private List<TicketResponse> tickets = new ArrayList<>();

    public BookingSummaryResponse() {
    }

    public BookingSummaryResponse(Long id, String bookingReference, Long userId, String userName, String userEmail,
                                  Long tripId, String origin, String destination, TransitMode transitMode,
                                  LocalDateTime departureTime, LocalDateTime arrivalTime, Integer seatCount,
                                  BigDecimal totalFare, PaymentMethod paymentMethod, BigDecimal voucherAmountUsed,
                                  BigDecimal walletAmountUsed, BookingStatus status, LocalDateTime bookingDate,
                                  List<TicketResponse> tickets) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.tripId = tripId;
        this.origin = origin;
        this.destination = destination;
        this.transitMode = transitMode;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.seatCount = seatCount;
        this.totalFare = totalFare;
        this.paymentMethod = paymentMethod;
        this.voucherAmountUsed = voucherAmountUsed;
        this.walletAmountUsed = walletAmountUsed;
        this.status = status;
        this.bookingDate = bookingDate;
        this.tickets = tickets != null ? tickets : new ArrayList<>();
    }

    public static BookingSummaryResponse fromEntity(Booking booking, BigDecimal voucherAmountUsed, BigDecimal walletAmountUsed) {
        if (booking == null) {
            return null;
        }

        List<TicketResponse> ticketResponses = booking.getTickets() != null
                ? booking.getTickets().stream().map(TicketResponse::fromEntity).toList()
                : new ArrayList<>();

        return new BookingSummaryResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getUser() != null ? booking.getUser().getId() : null,
                booking.getUser() != null ? booking.getUser().getName() : null,
                booking.getUser() != null ? booking.getUser().getEmail() : null,
                booking.getTrip() != null ? booking.getTrip().getId() : null,
                booking.getTrip() != null && booking.getTrip().getRoute() != null ? booking.getTrip().getRoute().getOrigin() : null,
                booking.getTrip() != null && booking.getTrip().getRoute() != null ? booking.getTrip().getRoute().getDestination() : null,
                booking.getTrip() != null && booking.getTrip().getRoute() != null ? booking.getTrip().getRoute().getMode() : null,
                booking.getTrip() != null ? booking.getTrip().getDepartureTime() : null,
                booking.getTrip() != null ? booking.getTrip().getArrivalTime() : null,
                booking.getSeatCount(),
                booking.getTotalFare(),
                booking.getPaymentMethod(),
                voucherAmountUsed != null ? voucherAmountUsed : BigDecimal.ZERO.setScale(2),
                walletAmountUsed != null ? walletAmountUsed : booking.getTotalFare(),
                booking.getStatus(),
                booking.getBookingDate(),
                ticketResponses
        );
    }

    public static BookingSummaryResponse fromEntity(Booking booking) {
        return fromEntity(booking, BigDecimal.ZERO, booking != null ? booking.getTotalFare() : BigDecimal.ZERO);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public TransitMode getTransitMode() {
        return transitMode;
    }

    public void setTransitMode(TransitMode transitMode) {
        this.transitMode = transitMode;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public BigDecimal getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(BigDecimal totalFare) {
        this.totalFare = totalFare;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getVoucherAmountUsed() {
        return voucherAmountUsed;
    }

    public void setVoucherAmountUsed(BigDecimal voucherAmountUsed) {
        this.voucherAmountUsed = voucherAmountUsed;
    }

    public BigDecimal getWalletAmountUsed() {
        return walletAmountUsed;
    }

    public void setWalletAmountUsed(BigDecimal walletAmountUsed) {
        this.walletAmountUsed = walletAmountUsed;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<TicketResponse> getTickets() {
        return tickets;
    }

    public void setTickets(List<TicketResponse> tickets) {
        this.tickets = tickets;
    }
}
