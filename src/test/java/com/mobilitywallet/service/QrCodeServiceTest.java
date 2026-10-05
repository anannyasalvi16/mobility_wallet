package com.mobilitywallet.service;

import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.service.impl.QrCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeServiceImpl();
    }

    @Test
    @DisplayName("Generates valid Base64 data URI string with PNG header")
    void generateQrCodeDataUri() {
        String payload = "https://mobilitywallet.com/verify?code=VCH-2026-TEST";
        String dataUri = qrCodeService.generateQrCodeDataUri(payload, 200, 200);

        assertThat(dataUri).isNotNull();
        assertThat(dataUri).startsWith("data:image/png;base64,");
        assertThat(dataUri.length()).isGreaterThan(100);
    }

    @Test
    @DisplayName("Generates valid PNG image bytes with standard PNG magic signature")
    void generateQrCodeImageBytes() {
        String payload = "VCH-2026-PAYLOAD-BYTES";
        byte[] pngBytes = qrCodeService.generateQrCodeImage(payload, 200, 200);

        assertThat(pngBytes).isNotNull();
        assertThat(pngBytes.length).isGreaterThan(50);

        // Standard PNG Magic Number: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
        assertThat(pngBytes[0]).isEqualTo((byte) 0x89);
        assertThat(pngBytes[1]).isEqualTo((byte) 0x50); // 'P'
        assertThat(pngBytes[2]).isEqualTo((byte) 0x4E); // 'N'
        assertThat(pngBytes[3]).isEqualTo((byte) 0x47); // 'G'
    }

    @Test
    @DisplayName("Formats structured JSON payload for voucher containing code, value, and expiry")
    void generateVoucherPayload() {
        User admin = new User("Admin", "admin@corp.com", "hash", null, Role.ROLE_ADMIN);
        User commuter = new User("Commuter", "user@corp.com", "hash", null, Role.ROLE_USER);
        commuter.setId(42L);

        Voucher voucher = new Voucher("VCH-2026-DEMO01", new BigDecimal("450.00"), LocalDate.of(2026, 12, 31), admin);
        voucher.setAssignedTo(commuter);

        String payload = qrCodeService.generateVoucherPayload(voucher);

        assertThat(payload).contains("\"type\":\"VOUCHER\"");
        assertThat(payload).contains("\"code\":\"VCH-2026-DEMO01\"");
        assertThat(payload).contains("\"value\":450.00");
        assertThat(payload).contains("\"expiry\":\"2026-12-31\"");
        assertThat(payload).contains("\"uid\":42");
    }
}
