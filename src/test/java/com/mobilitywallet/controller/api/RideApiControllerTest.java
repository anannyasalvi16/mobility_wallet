package com.mobilitywallet.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.repository.RideRepository;
import com.mobilitywallet.repository.TransactionRepository;
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
class RideApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User commuter;

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
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        for (String code : new String[]{"VCH-API-REDEEM-1", "VCH-API-REDEEM-DUP"}) {
            transactionRepository.findByVoucherCode(code).forEach(transactionRepository::delete);
            rideRepository.findByVoucherCode(code).ifPresent(rideRepository::delete);
            voucherRepository.findByCode(code).ifPresent(voucherRepository::delete);
        }
    }

    private Voucher createVoucher(String code, BigDecimal value) {
        // Clean up any old occurrences
        transactionRepository.findByVoucherCode(code).forEach(transactionRepository::delete);
        rideRepository.findByVoucherCode(code).ifPresent(rideRepository::delete);
        voucherRepository.findByCode(code).ifPresent(voucherRepository::delete);

        Voucher v = new Voucher(code, value, LocalDate.now().plusMonths(1), admin);
        v.setAssignedTo(commuter);
        v.setStatus(VoucherStatus.ASSIGNED);
        return voucherRepository.saveAndFlush(v);
    }

    @Test
    @DisplayName("POST /api/rides/redeem returns 201 Created and persists ride record on valid input")
    void testSuccessfulRedeemEndpoint() throws Exception {
        Voucher v = createVoucher("VCH-API-REDEEM-1", new BigDecimal("500.00"));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Santosh Patil",
                "MH-02-EE-1122",
                "Andheri East",
                "Churchgate",
                new BigDecimal("380.00")
        );

        mockMvc.perform(post("/api/rides/redeem")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voucherCode").value("VCH-API-REDEEM-1"))
                .andExpect(jsonPath("$.data.driverName").value("Santosh Patil"))
                .andExpect(jsonPath("$.data.vehicleNumber").value("MH-02-EE-1122"))
                .andExpect(jsonPath("$.data.fare").value(380.00));
    }

    @Test
    @DisplayName("POST /api/rides/redeem returns 409 Conflict when attempting to redeem an already-redeemed voucher")
    void testDuplicateRedemptionReturns409() throws Exception {
        Voucher v = createVoucher("VCH-API-REDEEM-DUP", new BigDecimal("500.00"));

        TaxiRedeemRequest request = new TaxiRedeemRequest(
                v.getCode(),
                "Driver One",
                "MH-01-AA-1111",
                "Loc A",
                "Loc B",
                new BigDecimal("300.00")
        );

        // First redemption succeeds
        mockMvc.perform(post("/api/rides/redeem")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second redemption with same voucher returns 409 Conflict
        mockMvc.perform(post("/api/rides/redeem")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/rides/redeem returns 400 Bad Request when validation constraints fail")
    void testValidationFailureReturns400() throws Exception {
        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "", // Blank voucher code
                "", // Blank driver name
                "", // Blank vehicle number
                "",
                "",
                new BigDecimal("-50.00") // Negative fare
        );

        mockMvc.perform(post("/api/rides/redeem")
                .with(httpBasic("user@mobility.com", "user123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/rides/redeem returns 401 Unauthorized without credentials")
    void testUnauthenticatedRedemptionReturns401() throws Exception {
        TaxiRedeemRequest request = new TaxiRedeemRequest(
                "VCH-SOME-CODE",
                "Driver",
                "MH-01-AB-1234",
                "A",
                "B",
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/rides/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/rides/my returns 200 OK with list of commuter rides")
    void testGetMyRidesReturns200() throws Exception {
        mockMvc.perform(get("/api/rides/my")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/rides requires ROLE_ADMIN: 200 for admin, 403 Forbidden for commuter")
    void testAdminRidesEndpointAccessControl() throws Exception {
        // Admin gets 200 OK
        mockMvc.perform(get("/api/rides")
                .with(httpBasic("admin@mobility.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Commuter gets 403 Forbidden
        mockMvc.perform(get("/api/rides")
                .with(httpBasic("user@mobility.com", "user123")))
                .andExpect(status().isForbidden());
    }
}
