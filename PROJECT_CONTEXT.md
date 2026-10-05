# Project Context — Mobility Wallet

**Last Updated:** All 8 Milestones Completed (100% Tested & Verified)  
**Technology Stack:** Java 17 | Spring Boot 3.2.4 | Maven | MySQL 8.0 | Thymeleaf | ZXing | SpringDoc OpenAPI  
**Repository:** https://github.com/anannyasalvi16/mobility_wallet  
**Branch:** main  

---

## 1. Architectural Decisions & Principles

1. **Precision & Money Math:**
   - All financial amounts (`balance`, `voucher.initialValue`, `fare`, `transaction.amount`) use `java.math.BigDecimal` mapped to MySQL `DECIMAL(12,2)`.
   - Floating-point types (`float`, `double`) are strictly prohibited for currency values.

2. **Concurrency & Race Condition Defenses:**
   - **Voucher Double-Redeem Prevention:** The `Voucher` entity utilizes optimistic locking via Hibernate's `@Version private Long version`. Redemption updates are executed inside an atomic `@Transactional` boundary checking `status == ASSIGNED`. Concurrent attempts fail with `OptimisticLockingFailureException`.
   - **Transit Seat Overbooking Prevention:** The `Trip` entity maintains `availableSeats` guarded by `@Version private Long version`. When reservations collide, an optimistic locking retry mechanism (up to 3 attempts with exponential jitter) re-checks seat availability before rejecting.

3. **Ledger Immutability:**
   - The `Transaction` table is strictly append-only.
   - Every wallet debit, voucher redemption, booking expense, and cancellation refund writes a new timestamped row.
   - Records are never updated or deleted.

4. **Security & Access Control (Dual SecurityFilterChain):**
   - **Filter Chain 1 (`@Order(1)` for `/api/**`):** Stateless REST API session management (`SessionCreationPolicy.STATELESS`), CSRF disabled, HTTP Basic authentication, custom `RestAuthenticationEntryPoint` returning 401 JSON, and custom `RestAccessDeniedHandler` returning 403 JSON.
   - **Filter Chain 2 (`@Order(2)` for Web UI):** Session-based form login at `/login`, logout at `/logout`, CSRF enabled, and `RoleBasedAuthenticationSuccessHandler` dynamically redirecting `ROLE_ADMIN` to `/admin/dashboard` and `ROLE_USER` to `/user/dashboard`.
   - **Role Segregation:** Strict role checking via `hasRole("ADMIN")` and `hasRole("USER")`. Admins receive a friendly `403.html` on commuter `/user/**` routes, and commuters receive `403` on `/admin/**`.
   - **Registration Invariance:** `UserService.register(...)` strictly forces `ROLE_USER` and auto-provisions a `Wallet` with `0.00` balance atomically. A caller cannot supply a role.

---

## 2. Milestone Execution Status

- [x] **Step 1: Project Skeleton, Configuration & Health Check**
  - Configured `pom.xml` with Spring Boot 3.2.4, Java 17, Spring Data JPA, MySQL, Spring Security, Thymeleaf, ZXing, and SpringDoc OpenAPI.
  - Setup `.gitignore`, `application.properties` (with environment variable fallbacks), and in-memory test properties.
  - Established package skeleton and implemented initial `HealthCheckController` and `OpenApiConfig`.
  - Added JUnit test suite verifying application context loading and `/api/health` response.
- [x] **Step 2: Core JPA Entities & Spring Data Repositories**
  - Implemented all 9 JPA domain entities (`User`, `Wallet`, `Voucher`, `Transaction`, `Route`, `Trip`, `Booking`, `Ticket`, `Ride`) and 6 domain Enums (`Role`, `VoucherStatus`, `TransactionType`, `TransitMode`, `PaymentMethod`, `BookingStatus`).
  - Added `@Version` optimistic locking columns on `Wallet`, `Voucher`, and `Trip`.
  - Implemented 9 Spring Data JPA repositories with query methods for voucher lookups, trip route searches, and transaction ledger summaries.
  - Built comprehensive slice tests (`EntityAndRepositoryTests`) verifying relational mappings, cascading, version increments, and ledger queries. (Tests run: 8, Failures: 0, Errors: 0).
- [x] **Step 3: Spring Security, User Registration, Session Auth & Initial Seed Data**
  - Configured dual `SecurityFilterChain` architecture (`@Order(1)` stateless API + `@Order(2)` session-based web).
  - Implemented `RoleBasedAuthenticationSuccessHandler` redirecting admins to `/admin/dashboard` and users to `/user/dashboard`.
  - Configured JSON 401 (`RestAuthenticationEntryPoint`) and JSON 403 (`RestAccessDeniedHandler`) for `/api/**`.
  - Implemented `UserService` and `RegistrationDto` with min-8 password validation, strict `ROLE_USER` assignment, and atomic wallet auto-creation at `0.00`.
  - Implemented idempotent `DataInitializer` (`@Profile({"dev", "test"})`) reading dev-only credentials from `application-dev.properties` and seeding admin, user, routes ("Campus to Tech Park", "Metro Station to Airport"), and scheduled trips.
  - Created Thymeleaf views for `/login`, `/register`, `/403`, `/admin/dashboard`, and `/user/dashboard` with plain CSS.
  - Built automated security and initialization test suites. (Tests run: 32, Failures: 0, Errors: 0).
- [x] **Step 4: Wallet & Voucher Engine with ZXing QR Code Generation**
  - Implemented `QrCodeService` using ZXing (`QRCodeWriter`) generating verifiable Base64 data URIs and raw PNG byte arrays.
  - Implemented `WalletService` with transactional `credit` and `debit` operations, strictly enforcing non-negative balances with `InsufficientBalanceException` and logging immutable `Transaction` ledger records.
  - Implemented `VoucherService` supporting single voucher creation (`VCH-YYYY-XXXXXX`), bulk batch creation, assignment to commuters (`CREATED` $\rightarrow$ `ASSIGNED`), lazy expiration detection, and automated midnight sweeps (`ScheduledTasksConfig`).
  - Added repository `JOIN FETCH` queries to prevent `LazyInitializationException` and $N+1$ queries with `open-in-view=false`.
  - Implemented REST controllers `VoucherApiController` and `WalletApiController`.
  - Built comprehensive unit and integration tests across `QrCodeServiceTest`, `WalletServiceTest`, `VoucherServiceTest`, and `VoucherAndWalletIntegrationTest`. (Tests run: 52, Failures: 0, Errors: 0).
- [x] **Step 5: Taxi Ride QR Redemption & Double-Redeem Concurrency Test**
  - Created `TaxiRedeemRequest` and `RideResponse` DTOs with strict validation annotations and OpenAPI schemas.
  - Implemented `RideService` and `RideServiceImpl` with `@Transactional` redemption: checks status is `ASSIGNED`, non-expired, validates meter fare against initial voucher value, transitions voucher to `REDEEMED`, persists `Ride`, and logs immutable `DEBIT` entry in the `Transaction` ledger.
  - Guarded against concurrency attacks via `@Version` optimistic locking on `Voucher` and unique constraints on `rides.voucher_id`, translating collisions to `VoucherAlreadyRedeemedException` and HTTP 409 Conflict.
  - Implemented `RideApiController` exposing `POST /api/rides/redeem`, `GET /api/rides/my`, `GET /api/rides/{id}`, `GET /api/rides/voucher/{code}`, and `GET /api/rides` (Admin paginated).
  - Configured `GlobalExceptionHandler` with `@ExceptionHandler(AccessDeniedException.class)` returning standard 403 Forbidden JSON.
  - Enhanced `RideRepository` and `TransactionRepository` with `JOIN FETCH` queries (`findByVoucherCode`, `findByIdWithDetails`) to eliminate lazy proxy issues under `open-in-view=false`.
  - Implemented mathematical concurrency test `VoucherDoubleRedeemConcurrencyTest` (10 threads spawned via `ExecutorService` using `CountDownLatch(1)` firing trigger simultaneously attempting redemption on the exact same voucher): asserts exactly 1 succeeds, exactly 9 fail with optimistic lock/conflict exceptions, and DB contains exactly 1 `Ride` and 1 debit ledger entry.
  - Implemented unit and integration tests across `RideServiceTest` and `RideApiControllerTest`. (Tests run: 67, Failures: 0, Errors: 0).
- [x] **Step 6: Transit Engine, Seat Booking with Optimistic Lock & Overbooking Concurrency Test**
  - Created transit DTOs: `BookingRequest`, `RouteCreateRequest`, `TripCreateRequest`, `TripSearchResponse`, `TicketResponse`, and `BookingSummaryResponse`.
  - Implemented `TransitBookingExecutor` using `Propagation.REQUIRES_NEW` to isolate transactional seat reservation attempts and support clean optimistic lock retry loops.
  - Implemented `TransitService` and `TransitServiceImpl`:
    - Schedule search by origin, destination, and travel date.
    - Seat booking with `@Version` optimistic locking retry loop (up to 3 attempts with exponential jitter backoff on `Trip.availableSeats`).
    - Payment flexibility: full digital wallet settlement, full voucher settlement, or split payment (voucher face value + remaining balance deducted from wallet).
    - Individual ticket generation with unique ticket numbers and ZXing-rendered Base64 QR code data URIs.
    - Automated cancellation flow: validates future departure, sets `CANCELLED` status, replenishes available seats on the trip, and automatically credits 100% of the booking fare back into the commuter's wallet.
  - Implemented `TransitApiController` exposing `/api/transit/**` endpoints for public route/schedule searching, commuter seat reservations/cancellations/ticket QR retrieval, and admin route/trip management.
  - Enhanced `TripRepository`, `BookingRepository`, and `TicketRepository` with fetch-joined queries (`searchAvailableTrips`, `findByIdWithDetails`, `findByIdWithRoute`) to avoid `LazyInitializationException` with Open-In-View disabled.
  - Permitted public `GET` access for route listing and schedule searching in `SecurityConfig.java`.
  - Implemented `SeatOverbookingConcurrencyTest`: Trip configured with 1 seat, 10 concurrent threads attempting to book simultaneously with `CountDownLatch(1)` trigger. Mathematically verified: exactly 1 booking confirmed, exactly 9 fail with `SeatOverbookedException`, and `availableSeats` remains strictly 0 (never negative).
  - Built unit and integration tests across `TransitServiceTest` and `TransitApiControllerTest`. (Tests run: 83, Failures: 0, Errors: 0).
- [x] **Step 7: Server-Rendered Thymeleaf Web Portal & Admin Analytics Dashboard**
  - Designed responsive Vanilla CSS design system (`main.css`) with curated color tokens (`--primary: #4f46e5`, `--accent: #0ea5e9`, `--success: #10b981`), glassmorphic cards, responsive data tables with status badges, and interactive modals.
  - Implemented `AnalyticsService` and `AnalyticsServiceImpl` aggregating real-time platform statistics (vouchers issued/redeemed, wallet liability, 7-day daily spend trend, route revenues, and seat load factors).
  - Built comprehensive **Commuter Portal** (`/user/dashboard`):
    - Real-time wallet balance card with auto-credit/refund audit indicators.
    - Assigned travel voucher cards with interactive modal displaying verifiable Base64 QR code.
    - Taxi Ride Redemption simulator modal for on-demand meter fare settlements.
    - Scheduled trip discovery by origin/destination/date with seat capacity bars and 1-click booking modal (wallet vs. voucher payment).
    - "My Bookings & Tickets" tab with individual ticket verification QR modal and 1-click cancellation with 100% wallet refund.
    - Immutable audit ledger table with color-coded `CREDIT`/`DEBIT` badges and voucher references.
  - Built comprehensive **Admin Portal** (`/admin/dashboard`):
    - 4 Chart.js analytics graphs (daily expenditure velocity, voucher status distribution, route revenues, seat load factor occupancy).
    - Single voucher issuance form and bulk batch generator (`VCH-YYYY-XXXXXX`).
    - Voucher assignment interface for registered commuters.
    - Transit route creator and trip scheduler.
    - Live taxi ride settlement audit trail.
  - Built modern Public/Auth pages: Landing page (`index.html`), Sign In (`auth/login.html`) with 1-click demo test credentials autofill, Registration (`auth/register.html`), and 403 Forbidden page (`error/403.html`).
  - Added fetch-joined queries in `VoucherRepository` (`findAllWithUsers`) and `TransactionRepository` (`findByWalletUserIdWithDetailsOrderByCreatedAtDesc`) avoiding `LazyInitializationException` under `open-in-view=false`.
  - Built automated web integration test suite `WebPortalIntegrationTest` validating views, models, CSRF protections, and form actions. (Total tests run: 94, Failures: 0, Errors: 0).
- [x] **Step 8: Complete REST API, OpenAPI/Swagger Documentation & Interview Prep Guide**
  - Enriched `AdminApiController` (`GET /api/admin/analytics`, `GET /api/admin/commuters`).
  - Enriched `UserApiController` (`GET /api/user/profile`, `GET /api/user/vouchers`, `GET /api/user/active-vouchers`).
  - Updated `OpenApiConfig` with HTTP Basic Authentication SecurityScheme component and tag groupings.
  - Implemented `AdminAndUserApiControllerTest` with 8 comprehensive integration tests verifying role authorization, profile retrieval, voucher queries, admin analytics, and `/v3/api-docs` schema generation.
  - Authored production-ready `README.md` containing executive summary, system architecture diagrams, relational ER diagram (Mermaid), local setup instructions for MySQL Workbench, demo accounts table, REST API reference with sample cURL commands, web UI walkthrough, and an in-depth Technical Interview Preparation Guide.
  - Executed hermetic test suite: 102 tests run, 0 failures.
- [x] **Step 9: Admin Analytics Dashboard Polish**
  - Added centered "No data yet" overlay state for empty datasets across all Chart.js visualizations.
  - Formatted y-axes to start at 0 with integer ticks and formatted rupee (`₹`) labels.
  - Renamed "Total Vouchers Issued" to "Total Voucher Value Issued" with active voucher count in subtitle.
  - Added dedicated tooltip and subtitle defining "Total Wallet Liability" (unspent active voucher liabilities + unspent wallet balances).
  - Enhanced voucher status donut chart with detailed tooltips displaying count and percentage shares.
  - Added live "Recent Activity" ledger card displaying the 5 most recent platform transactions.
  - Added unit test in `AnalyticsServiceTest` verifying recent activity transaction retrieval. (Total tests: 103).
- [x] **Step 10: Mobile Responsiveness & Logo Branding Options (Phase 1 Choice Point)**
  - **Task 1: Mobile Responsiveness:**
    - Integrated responsive CSS design system in `main.css` targeting 360px small mobile, 768px tablet, and $\ge 1024$px desktop viewports.
    - Added touch-friendly targets ($\ge 44$px min-height/width on buttons, form controls, and tab selectors).
    - Horizontally scrolling table containers (`.table-responsive` with `min-width: 620px`) preventing squishing on small screens.
    - Large, crisp, centered QR code preview (`max-width: 280px; width: 100%; aspect-ratio: 1/1; image-rendering: crisp-edges;`).
    - Added project attribution footer: *"Made by Anannya Salvi • 2026"* across all Thymeleaf templates.
  - **Task 2: Brand Identity & Vector Logos (Phase 1):**
    - Created `src/main/resources/static/css/theme.css` with zero hardcoded colors elsewhere:
      - `--color-primary: #0F172A;` (deep navy)
      - `--color-accent: #FACC15;` (taxi yellow)
      - `--color-bg: #F8FAFC;` (light background)
      - `--color-text: #0F172A;` (text on light)
      - `--color-text-on-dark: #F8FAFC;` (text on navy)
    - Handcrafted 12 distinct SVG logo files in `src/main/resources/static/images/logo-options/` with zero hardcoded hex codes:
      - **Option A ("Taxi Light"):** Taxi roof-light sign in navy with bold rupee symbol (₹) centred in yellow, 2 mounting feet, and lit ray lines. (Light, Dark, and Horizontal wordmark variants).
      - **Option B ("Taxi Wallet"):** Navy wallet outline with curved flap, side clasp, and recognisable side-view taxi inside (yellow roof light, cabin, two wheels). (Light, Dark, and Horizontal wordmark variants).
      - **Option C ("Taxi Wallet Badge"):** Option B concept inside rounded-square app-icon container with solid navy background and yellow taxi — tailored for favicons and mobile home-screens. (Light, Dark, and Horizontal wordmark variants).
    - Built `/logo-preview` preview page showcasing Options A, B, and C at Large (96px), Medium (48px), and Small (32px) on both Light and Dark panels.
    - Hermetic test suite: **106 tests run, 0 failures, 0 errors, 0 skipped**.
- [x] **Step 11: Production Deployment Readiness, Containerization & Cloud Guide**
  - Created `src/main/resources/application-prod.properties`:
    - Sourced DB URL (`${DB_URL}`), username (`${DB_USERNAME}`), and password (`${DB_PASSWORD}`) strictly from environment variables.
    - Sourced `server.port` from `${PORT:8080}`.
    - Set `spring.jpa.hibernate.ddl-auto=update`, `show-sql=false`, and `open-in-view=false`.
    - Hardened error pages: `server.error.include-stacktrace=never`, `server.error.include-message=never`, `server.error.include-exception=false`.
    - Enabled reverse proxy HTTPS headers: `server.forward-headers-strategy=framework`.
  - Hardened `DataInitializer`:
    - Supports `prod` profile alongside `dev` and `test`.
    - In `prod`, seeds **ONLY** an administrator whose credentials are supplied via `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables.
    - Strictly disables commuter demo user (`user@mobility.com`) and mock data (routes, trips, vouchers) in `prod`.
    - Actively detects and rejects dev fallback passwords (`admin123`/`user123`).
  - Added Spring Boot Actuator dependency and exposed `/actuator/health` (permitAll) in `SecurityConfig.java` for cloud host liveness and readiness probes.
  - Added multi-stage `Dockerfile`:
    - Stage 1 (Builder): `maven:3.9.6-eclipse-temurin-17-alpine` caching dependency layers and building executable JAR.
    - Stage 2 (Runtime): Minimal `eclipse-temurin:17-jre-alpine` running as unprivileged `appuser:appgroup` with container-aware JVM flags and healthcheck probe.
  - Created `.dockerignore` and enhanced `.gitignore` to strictly exclude `.env`, `.env.*`, `.env.local`, `target/`, and IDE files.
  - Authored comprehensive `DEPLOYMENT.md` covering step-by-step deployment to Railway and Render, Docker CLI instructions, environment variables matrix, and local smoke testing.
  - Added automated test suites (`DataInitializerProdTest`, `WebSecurityTest.actuatorHealthEndpointIsPermitted`).
  - All 108 tests passing hermetically (0 failures, 0 errors, 0 skipped).
- [x] **Step 12: Enterprise Frontend Overhaul & Elimination of AI-Slop / Vibe-Coded Aesthetics**
  - **Em Dash Elimination:** Fully removed all em dashes (`—`) across titles, headers, descriptions, tables, and footers, replacing with crisp pipe `|` or hyphen `-` separators.
  - **Geometry & Border Radius Hardening:** Replaced bubbly rounded pills (`9999px`) and round corners with crisp enterprise geometry: `--radius-xs: 2px`, `--radius-sm: 2px`, `--radius-md: 4px`, `--radius-lg: 4px`, `--radius-full: 2px`.
  - **Surface & Hover Physics Restraint:** Completely removed bouncy `translateY` hover lifts and glowing drop-shadows on stationary containers (`.stat-card`, `.voucher-card`, `.feature-card`, `.table-card`).
  - **Emoji & Icon Overkill Purge:** Stripped all arbitrary emojis (`🚊`, `👤`, `🛡️`, `⚡`, `⚠️`, `💳`, `🎟️`, `🚌`, `📊`, `💵`, `ℹ️`, `📈`, `💰`, `📦`, `🛣️`, `🕒`, `🚕`, `📱`, `🔍`, `✓`) across navbars, KPI metric cards, dashboard tabs, action buttons, table columns, modals, and empty states.
  - **Brand Mark & Favicon Cohesion:** Integrated official hand-crafted SVG `logo.svg` into navbars, added proper SVG and PNG favicons (`/favicon.svg`, `/favicon.png`) with theme-color meta `#0F172A` to every template.
  - **100% Test Pass Rate:** 108 tests passing hermetically with 0 failures, 0 errors.

---

## 3. Active Configuration Contracts

- **Author & Year:** Made by Anannya Salvi • 2026
- **Local Port:** `8080` (overridable via `PORT`)
- **Landing Page URL:** `http://localhost:8080/`
- **Wi-Fi Mobile URL:** `http://192.168.29.155:8080/`
- **Health Check Endpoints:**
  - Standard Actuator: `http://localhost:8080/actuator/health`
  - Legacy REST API: `http://localhost:8080/api/health`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Database URL (Dev):** `jdbc:mysql://localhost:3306/mobility_wallet` (MySQL 8.0, user `root`, password `Password`)
- **Database URL (Prod):** 100% sourced from `${DB_URL}` (no hardcoded fallback)
- **H2 In-Memory URL:** Active for hermetic `@ActiveProfiles("test")` unit tests (108 passing tests across 19 test suites)
- **Container Image:** Built via multi-stage `Dockerfile` with slim Java 17 JRE Alpine runtime and non-root user.



