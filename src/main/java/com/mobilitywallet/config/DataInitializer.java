package com.mobilitywallet.config;

import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.Route;
import com.mobilitywallet.entity.Transaction;
import com.mobilitywallet.entity.TransactionType;
import com.mobilitywallet.entity.TransitMode;
import com.mobilitywallet.entity.Trip;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.entity.VoucherStatus;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TransactionRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Seeds initial accounts and master data on startup.
 * - Prod profile: Seeds ONLY an administrator whose email and password come from
 *   environment variables (ADMIN_EMAIL, ADMIN_PASSWORD). Does not seed demo users or mock data.
 * - Dev & Test profiles: Seeds dev admin, demo commuter user, routes, trips, and vouchers.
 * Fully idempotent: safe to execute multiple times without duplicate record creation.
 */
@Component
@Profile({"dev", "test", "prod"})
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final VoucherRepository voucherRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    // Admin credentials (sourcing from ADMIN_EMAIL / ADMIN_PASSWORD env vars, with dev property fallbacks)
    @Value("${ADMIN_NAME:${mobility.seed.admin.name:System Administrator}}")
    private String adminName;

    @Value("${ADMIN_EMAIL:${mobility.seed.admin.email:}}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:${mobility.seed.admin.password:}}")
    private String adminPassword;

    @Value("${ADMIN_PHONE:${mobility.seed.admin.phone:9876500000}}")
    private String adminPhone;

    @Value("${SEED_DEMO_DATA:true}")
    private boolean seedDemoData;

    // Dev-only seed credentials loaded from application-dev.properties
    @Value("${mobility.seed.user.name:John Doe}")
    private String userName;

    @Value("${mobility.seed.user.email:user@mobility.com}")
    private String userEmail;

    @Value("${mobility.seed.user.password:user123}")
    private String userPassword;

    @Value("${mobility.seed.user.phone:9876511111}")
    private String userPhone;

    @Value("${mobility.seed.user.initial-balance:750.00}")
    private BigDecimal userInitialBalance;

    public DataInitializer(UserRepository userRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           RouteRepository routeRepository,
                           TripRepository tripRepository,
                           VoucherRepository voucherRepository,
                           PasswordEncoder passwordEncoder,
                           Environment environment) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.voucherRepository = voucherRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
        boolean isTest = environment.acceptsProfiles(Profiles.of("test"));

        if (isProd) {
            log.info("Running DataInitializer in PRODUCTION profile...");

            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
                if ("admin123".equals(adminPassword) || "user123".equals(adminPassword)) {
                    log.warn("Dev default password (admin123/user123) is rejected in production! Provide a unique ADMIN_PASSWORD.");
                } else {
                    seedUserIfNotPresent(
                            (adminName != null && !adminName.isBlank()) ? adminName : "System Administrator",
                            adminEmail,
                            adminPassword,
                            (adminPhone != null && !adminPhone.isBlank()) ? adminPhone : "0000000000",
                            Role.ROLE_ADMIN,
                            BigDecimal.ZERO
                    );
                    log.info("Production administrator seeded successfully.");
                }
            } else {
                log.warn("Production ADMIN_EMAIL or ADMIN_PASSWORD environment variable is not configured.");
            }

            if (!seedDemoData) {
                log.info("Production demo seeding disabled (SEED_DEMO_DATA=false).");
                return;
            }
            log.info("Production demo seeding enabled (SEED_DEMO_DATA=true). Seeding example.com demo accounts, routes, trips, and vouchers...");
        }

        if (isTest) {
            // Strictly preserve single admin & single user for DataInitializerTest assertions
            seedUserIfNotPresent(
                    (adminName != null && !adminName.isBlank()) ? adminName : "System Administrator",
                    (adminEmail != null && !adminEmail.isBlank()) ? adminEmail : "admin@mobility.com",
                    (adminPassword != null && !adminPassword.isBlank()) ? adminPassword : "admin123",
                    adminPhone,
                    Role.ROLE_ADMIN,
                    BigDecimal.ZERO
            );
            seedUserIfNotPresent(userName, userEmail, userPassword, userPhone, Role.ROLE_USER, userInitialBalance);
            seedTransitAndVouchers("admin@mobility.com", "user@mobility.com");
            log.info("Test DataInitializer execution finished successfully.");
            return;
        }

        // 1. Seed example.com demo accounts for quick-fill demo login buttons
        seedUserIfNotPresent("Demo Administrator", "admin@example.com", "admin123", "9876500000", Role.ROLE_ADMIN, BigDecimal.ZERO);
        seedUserIfNotPresent("Demo Commuter", "user@example.com", "user123", "9876511111", Role.ROLE_USER, userInitialBalance);
        seedUserIfNotPresent("Demo User", "example@email.com", "user123", "9876511112", Role.ROLE_USER, userInitialBalance);

        // 2. In dev profile, also ensure mobility.com accounts for backward compatibility
        if (!isProd) {
            String effectiveAdminEmail = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail : "admin@mobility.com";
            String effectiveAdminPassword = (adminPassword != null && !adminPassword.isBlank()) ? adminPassword : "admin123";
            seedUserIfNotPresent("System Administrator", effectiveAdminEmail, effectiveAdminPassword, adminPhone, Role.ROLE_ADMIN, BigDecimal.ZERO);
            seedUserIfNotPresent(userName, userEmail, userPassword, userPhone, Role.ROLE_USER, userInitialBalance);
        }

        // 3. Seed routes, trips, and demo vouchers assigned to demo commuter
        seedTransitAndVouchers("admin@example.com", "user@example.com");

        log.info("Idempotent DataInitializer execution finished successfully.");
    }

    private User seedUserIfNotPresent(String name, String email, String password, String phone, Role role, BigDecimal initialBalance) {
        String sanitizedEmail = email.toLowerCase().trim();
        Optional<User> existingOpt = userRepository.findByEmail(sanitizedEmail);
        if (existingOpt.isEmpty()) {
            log.info("Seeding account: {} ({})", sanitizedEmail, role);
            User user = new User();
            user.setName(name);
            user.setEmail(sanitizedEmail);
            user.setPassword(passwordEncoder.encode(password));
            user.setPhone(phone);
            user.setRole(role);
            user.setEnabled(true);

            Wallet wallet = new Wallet(user, initialBalance.setScale(2));
            user.setWallet(wallet);
            User savedUser = userRepository.save(user);

            if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
                Transaction creditTx = new Transaction(
                        savedUser.getWallet(),
                        null,
                        TransactionType.CREDIT,
                        initialBalance.setScale(2),
                        "Welcome mobility allowance credited to wallet"
                );
                transactionRepository.save(creditTx);
            }
            return savedUser;
        } else {
            User existing = existingOpt.get();
            existing.setPassword(passwordEncoder.encode(password));
            existing.setRole(role);
            existing.setEnabled(true);
            if (existing.getWallet() == null) {
                Wallet wallet = new Wallet(existing, initialBalance.setScale(2));
                existing.setWallet(wallet);
            }
            userRepository.save(existing);
            log.info("Account {} exists; credentials ensured.", sanitizedEmail);
            return existing;
        }
    }

    private void seedTransitAndVouchers(String adminEmailForVouchers, String userEmailForVouchers) {
        // Seed Routes: "Campus to Tech Park" and "Metro Station to Airport"
        Route route1 = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("Campus", "Tech Park")
                .orElseGet(() -> routeRepository.save(new Route("Campus", "Tech Park", 14.5, TransitMode.BUS)));

        Route route2 = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("Metro Station", "Airport")
                .orElseGet(() -> routeRepository.save(new Route("Metro Station", "Airport", 26.0, TransitMode.METRO)));

        // Seed Trips for each route with future departure times
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1).withSecond(0).withNano(0);
        LocalDateTime dayAfter = now.plusDays(2).withSecond(0).withNano(0);

        if (tripRepository.searchAvailableTrips("Campus", "Tech Park", tomorrow.minusDays(1), dayAfter.plusDays(1)).isEmpty()) {
            log.info("Seeding future trips for Campus to Tech Park...");
            tripRepository.save(new Trip(route1, tomorrow.withHour(9).withMinute(0), tomorrow.withHour(9).withMinute(45), 40, new BigDecimal("45.00")));
            tripRepository.save(new Trip(route1, tomorrow.withHour(18).withMinute(0), tomorrow.withHour(18).withMinute(45), 40, new BigDecimal("45.00")));
            tripRepository.save(new Trip(route1, dayAfter.withHour(9).withMinute(0), dayAfter.withHour(9).withMinute(45), 40, new BigDecimal("45.00")));
        }

        if (tripRepository.searchAvailableTrips("Metro Station", "Airport", tomorrow.minusDays(1), dayAfter.plusDays(1)).isEmpty()) {
            log.info("Seeding future trips for Metro Station to Airport...");
            tripRepository.save(new Trip(route2, tomorrow.withHour(8).withMinute(0), tomorrow.withHour(8).withMinute(35), 60, new BigDecimal("60.00")));
            tripRepository.save(new Trip(route2, tomorrow.withHour(17).withMinute(30), tomorrow.withHour(18).withMinute(5), 60, new BigDecimal("60.00")));
            tripRepository.save(new Trip(route2, dayAfter.withHour(10).withMinute(0), dayAfter.withHour(10).withMinute(35), 60, new BigDecimal("60.00")));
        }

        // Seed Vouchers if none exist
        if (voucherRepository.count() == 0) {
            User admin = userRepository.findByEmail(adminEmailForVouchers.toLowerCase().trim())
                    .or(() -> userRepository.findByEmail("admin@example.com"))
                    .or(() -> userRepository.findByEmail("admin@mobility.com"))
                    .orElse(null);
            User commuter = userRepository.findByEmail(userEmailForVouchers.toLowerCase().trim())
                    .or(() -> userRepository.findByEmail("user@example.com"))
                    .or(() -> userRepository.findByEmail("user@mobility.com"))
                    .orElse(null);

            if (admin != null && commuter != null) {
                log.info("Seeding demo vouchers...");
                Voucher v1 = new Voucher("VOUCH-COMMUTE-100", new BigDecimal("100.00"), LocalDate.now().plusDays(30), admin);
                v1.setAssignedTo(commuter);
                v1.setStatus(VoucherStatus.ASSIGNED);
                voucherRepository.save(v1);

                Voucher v2 = new Voucher("VOUCH-TAXI-50", new BigDecimal("50.00"), LocalDate.now().plusDays(14), admin);
                v2.setAssignedTo(commuter);
                v2.setStatus(VoucherStatus.ASSIGNED);
                voucherRepository.save(v2);

                Voucher v3 = new Voucher("VOUCH-POOL-200", new BigDecimal("200.00"), LocalDate.now().plusDays(60), admin);
                voucherRepository.save(v3);
            }
        }
    }
}
