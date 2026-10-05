package com.mobilitywallet.dto.response;

import com.mobilitywallet.entity.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Details of an individual seat transit ticket")
public class TicketResponse {

    @Schema(description = "Ticket ID", example = "1")
    private Long id;

    @Schema(description = "Assigned seat number", example = "S12")
    private String seatNumber;

    @Schema(description = "Unique verifiable ticket number", example = "TKT-BKG-171800-01")
    private String ticketNumber;

    @Schema(description = "Base64 data URI QR code for conductor validation", example = "data:image/png;base64,...")
    private String qrCodeData;

    @Schema(description = "Validation status", example = "false")
    private boolean validated;

    @Schema(description = "Validation timestamp if validated", example = "2026-10-04T09:05:00")
    private LocalDateTime validatedAt;

    public TicketResponse() {
    }

    public TicketResponse(Long id, String seatNumber, String ticketNumber, String qrCodeData, boolean validated, LocalDateTime validatedAt) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.ticketNumber = ticketNumber;
        this.qrCodeData = qrCodeData;
        this.validated = validated;
        this.validatedAt = validatedAt;
    }

    public static TicketResponse fromEntity(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return new TicketResponse(
                ticket.getId(),
                ticket.getSeatNumber(),
                ticket.getTicketNumber(),
                ticket.getQrCodeData(),
                ticket.isValidated(),
                ticket.getValidatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public String getQrCodeDataUri() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }
}
