package com.mobilitywallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilitywallet.dto.request.VoucherBulkCreateRequest;
import com.mobilitywallet.dto.request.VoucherCreateRequest;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoucherAndWalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Voucher testVoucher;

    @BeforeEach
    void setUp() {
        User admin = userRepository.findByEmail("admin@mobility.com").orElseGet(() -> {
            User newAdmin = new User("System Administrator", "admin@mobility.com", passwordEncoder.encode("admin123"), "9876500000", Role.ROLE_ADMIN);
            Wallet w = new Wallet(newAdmin, BigDecimal.ZERO);
            newAdmin.setWallet(w);
            return userRepository.save(newAdmin);
        });

        User commuter = userRepository.findByEmail("user@mobility.com").orElseGet(() -> {
            User newUser = new User("John Doe", "user@mobility.com", passwordEncoder.encode("user123"), "9876511111", Role.ROLE_USER);
            Wallet w = new Wallet(newUser, new BigDecimal("750.00"));
            newUser.setWallet(w);
            return userRepository.save(newUser);
        });

        testVoucher = voucherRepository.findAll().stream()
                .filter(v -> v.getStatus() == VoucherStatus.ASSIGNED)
                .findFirst()
                .orElseGet(() -> {
                    Voucher v = new Voucher("VCH-TEST-ASSIGNED-1", new BigDecimal("350.00"), LocalDate.now().plusMonths(1), admin);
                    v.setAssignedTo(commuter);
                    v.setStatus(VoucherStatus.ASSIGNED);
                    return voucherRepository.save(v);
                });
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        voucherRepository.findByCode("VCH-TEST-ASSIGNED-1").ifPresent(voucherRepository::delete);
        voucherRepository.findAll().stream()
                .filter(v -> v.getCode().startsWith("VCH-") && !v.getCode().startsWith("VOUCH-"))
                .forEach(voucherRepository::delete);
    }

    @Test
    @DisplayName("Admin can create a single voucher via REST API")
    void adminCanCreateSingleVoucher() throws Exception {
        VoucherCreateRequest request = new VoucherCreateRequest(new BigDecimal("750.00"), LocalDate.now().plusMonths(2));

        mockMvc.perform(post("/api/vouchers")
                .with(httpBasic("admin@mobility.com", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.startsWith("VCH-")))
                .andExpect(jsonPath("$.data.initialValue").value(750.00))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    @DisplayName("Admin can create bulk vouchers via REST API")
    void adminCanCreateBulkVouchers() throws Exception {
        VoucherBulkCreateRequest request = new VoucherBulkCreateRequest(3, new BigDecimal("200.00"), LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/vouchers/bulk")
                .with(httpBasic("admin@mobility.com", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("Commuter can view their assigned vouchers via REST API")
    void commuterCanViewAssignedVouchers() throws Exception {
        mockMvc.perform(get("/api/vouchers/my")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Commuter can query their wallet balance via REST API")
    void commuterCanQueryWalletBalance() throws Exception {
        mockMvc.perform(get("/api/wallet/my")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").isNumber());
    }

    @Test
    @DisplayName("Commuter can fetch voucher QR code data URI via REST API")
    void fetchVoucherQrCode() throws Exception {
        mockMvc.perform(get("/api/vouchers/" + testVoucher.getCode() + "/qr")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(testVoucher.getCode()))
                .andExpect(jsonPath("$.data.qrDataUri").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }
}
