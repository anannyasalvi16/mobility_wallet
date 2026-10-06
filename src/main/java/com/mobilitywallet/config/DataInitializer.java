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

        if (isProd) {
            log.info("Running DataInitializer in PRODUCTION profile...");

            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.warn("Production ADMIN_EMAIL or ADMIN_PASSWORD environment variable is not configured. Skipping admin seeding.");
                return;
            }

            if ("admin123".equals(adminPassword) || "user123".equals(adminPassword)) {
                log.warn("Dev default password (admin123/user123) is rejected in production! Provide a unique ADMIN_PASSWORD.");
                return;
            }

            String sanitizedEmail = adminEmail.toLowerCase().trim();
            java.util.Optional<User> existingOpt = userRepository.findByEmail(sanitizedEmail);
            if (existingOpt.isEmpty()) {
                log.info("Seeding production administrator: {}", sanitizedEmail);
                User admin = new User();
                admin.setName(adminName != null && !adminName.isBlank() ? adminName : "System Administrator");
                admin.setEmail(sanitizedEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setPhone(adminPhone != null && !adminPhone.isBlank() ? adminPhone : "0000000000");
                admin.setRole(Role.ROLE_ADMIN);
                admin.setEnabled(true);

                Wallet adminWallet = new Wallet(admin, BigDecimal.ZERO.setScale(2));
                admin.setWallet(adminWallet);
                userRepository.save(admin);
                log.info("Production administrator seeded successfully.");
            } else {
                User existing = existingOpt.get();
                existing.setPassword(passwordEncoder.encode(adminPassword));
                existing.setRole(Role.ROLE_ADMIN);
                existing.setEnabled(true);
                userRepository.save(existing);
                log.info("Production administrator {} exists; password updated from environment.", sanitizedEmail);
            }

            // In prod: Seed ONLY an admin. Never create demo users, test vouchers, or mock routes.
            return;
        }

        // DEV / TEST Profile Seeding
        log.info("Running idempotent DataInitializer for DEV/TEST profile...");

        String effectiveAdminEmail = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail : "admin@mobility.com";
        String effectiveAdminPassword = (adminPassword != null && !adminPassword.isBlank()) ? adminPassword : "admin123";

        // 1. Seed Administrator (ROLE_ADMIN, with wallet) if missing
        if (!userRepository.existsByEmail(effectiveAdminEmail.toLowerCase().trim())) {
            log.info("Seeding dev admin: {}", effectiveAdminEmail);
            User admin = new User();
            admin.setName(adminName != null && !adminName.isBlank() ? adminName : "System Administrator");
            admin.setEmail(effectiveAdminEmail.toLowerCase().trim());
            admin.setPassword(passwordEncoder.encode(effectiveAdminPassword));
            admin.setPhone(adminPhone);
            admin.setRole(Role.ROLE_ADMIN);
            admin.setEnabled(true);

            Wallet adminWallet = new Wallet(admin, BigDecimal.ZERO.setScale(2));
            admin.setWallet(adminWallet);
            userRepository.save(admin);
        }

        // 2. Seed Commuter User (ROLE_USER, with wallet) if missing
        if (!userRepository.existsByEmail(userEmail.toLowerCase().trim())) {
            log.info("Seeding dev user: {}", userEmail);
            User user = new User();
            user.setName(userName);
            user.setEmail(userEmail.toLowerCase().trim());
            user.setPassword(passwordEncoder.encode(userPassword));
            user.setPhone(userPhone);
            user.setRole(Role.ROLE_USER);
            user.setEnabled(true);

            Wallet userWallet = new Wallet(user, userInitialBalance.setScale(2));
            user.setWallet(userWallet);
            User savedUser = userRepository.save(user);

            // Log initial credit transaction
            Transaction creditTx = new Transaction(
                    savedUser.getWallet(),
                    null,
                    TransactionType.CREDIT,
                    userInitialBalance.setScale(2),
                    "Dev welcome mobility allowance credited to wallet"
            );
            transactionRepository.save(creditTx);
        }

        // 3. Seed Routes: "Campus to Tech Park" and "Metro Station to Airport"
        Route route1 = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("Campus", "Tech Park")
                .orElseGet(() -> routeRepository.save(new Route("Campus", "Tech Park", 14.5, TransitMode.BUS)));

        Route route2 = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("Metro Station", "Airport")
                .orElseGet(() -> routeRepository.save(new Route("Metro Station", "Airport", 26.0, TransitMode.METRO)));

        // 4. Seed Trips for each route with future departure times computed from LocalDateTime.now()
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1).withSecond(0).withNano(0);
        LocalDateTime dayAfter = now.plusDays(2).withSecond(0).withNano(0);

        if (tripRepository.searchAvailableTrips("Campus", "Tech Park", tomorrow.minusDays(1), dayAfter.plusDays(1)).isEmpty()) {
            log.info("Seeding future trips for Campus to Tech Park...");
            // Tomorrow 9:00 AM
            tripRepository.save(new Trip(route1, tomorrow.withHour(9).withMinute(0), tomorrow.withHour(9).withMinute(45), 40, new BigDecimal("45.00")));
            // Tomorrow 6:00 PM
            tripRepository.save(new Trip(route1, tomorrow.withHour(18).withMinute(0), tomorrow.withHour(18).withMinute(45), 40, new BigDecimal("45.00")));
            // Day after tomorrow 9:00 AM
            tripRepository.save(new Trip(route1, dayAfter.withHour(9).withMinute(0), dayAfter.withHour(9).withMinute(45), 40, new BigDecimal("45.00")));
        }

        if (tripRepository.searchAvailableTrips("Metro Station", "Airport", tomorrow.minusDays(1), dayAfter.plusDays(1)).isEmpty()) {
            log.info("Seeding future trips for Metro Station to Airport...");
            // Tomorrow 8:00 AM
            tripRepository.save(new Trip(route2, tomorrow.withHour(8).withMinute(0), tomorrow.withHour(8).withMinute(35), 60, new BigDecimal("60.00")));
            // Tomorrow 5:30 PM
            tripRepository.save(new Trip(route2, tomorrow.withHour(17).withMinute(30), tomorrow.withHour(18).withMinute(5), 60, new BigDecimal("60.00")));
            // Day after tomorrow 10:00 AM
            tripRepository.save(new Trip(route2, dayAfter.withHour(10).withMinute(0), dayAfter.withHour(10).withMinute(35), 60, new BigDecimal("60.00")));
        }

        // 5. Seed Vouchers if none exist
        if (voucherRepository.count() == 0) {
            User admin = userRepository.findByEmail(effectiveAdminEmail.toLowerCase().trim()).orElse(null);
            User commuter = userRepository.findByEmail(userEmail.toLowerCase().trim()).orElse(null);
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

        log.info("Idempotent DataInitializer execution finished successfully.");
    }
}
