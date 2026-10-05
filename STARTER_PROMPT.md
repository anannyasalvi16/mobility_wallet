# Starter Prompt for your AI IDE (Antigravity / Cursor / Windsurf / Claude Code)

**Setup:** 
1. Make sure MySQL 8 is running locally (default database: `mobility_wallet`).
2. Keep `PRD.md` and `TRD.md` in the root of this folder.
3. Open this folder in your AI IDE, then copy and paste the prompt in the block below.

---

```text
You are my senior Java pair-programmer. We are building "Mobility Wallet", an enterprise-grade transit allowance voucher and ticketing platform.

Read PRD.md and TRD.md in this repository fully before writing any code. They are the single source of truth. If any requirement is ambiguous or conflicts with clean architecture, ask me before deciding.

## About Me
I am a B.E. IT student building this as my flagship resume/portfolio project. I run it locally with Java 17, Maven, and MySQL Workbench. I must be able to explain every file, database relationship, concurrency lock, and design pattern in a technical interview. Keep the code simple, readable, and cleanly commented where logic is non-obvious. No over-engineered abstractions, no unnecessary dependencies.

## Non-Negotiable Rules
1. Technology Stack: Java 17, Spring Boot 3, Maven, Spring Data JPA, MySQL 8, Thymeleaf with clean responsive CSS. No React, Vue, Angular, or separate JS build steps. Minimal vanilla JS only for charts and QR previews.
2. Financial Math: Use BigDecimal for all money calculations. Never use float or double for currency.
3. Concurrency Protection: Use optimistic locking (@Version) on Trip (seats) and Voucher (redemption) to guarantee zero overbooking and zero double-redemption. Back this with multi-threaded JUnit concurrency tests.
4. Layered Architecture: Strict package boundaries: controller, service, repository, entity, dto, config, exception. Global exception handler with @ControllerAdvice and Bean Validation on all inputs.
5. Dual Interface: Clean server-rendered Thymeleaf for browser users, plus an OpenAPI/Swagger documented REST API under /api.
6. Step-by-Step Execution: Work one step at a time. After each step, tell me:
   - What you built and why
   - How to run it locally
   - How to test it (unit tests or browser/cURL)
   Then STOP and wait for my explicit confirmation ("next") before moving forward.
7. Maintain PROJECT_CONTEXT.md: Create and update PROJECT_CONTEXT.md in the root, recording all design decisions, API contracts, and completed milestones. Re-read it at the start of every step.

---

## Step 1 (Do this now, nothing beyond it)
a) Create the Maven pom.xml with Java 17, Spring Boot 3.2+, Spring Data JPA, MySQL Connector/J, Spring Security, Thymeleaf, ZXing, SpringDoc OpenAPI, and Spring Boot Starter Validation.
b) Create .gitignore, application.properties (configured for MySQL with environment variable fallbacks), and the main Spring Boot Application class.
c) Create the layered package skeleton (controller, service, repository, entity, dto, config, exception) and a basic HealthCheckController.
d) Initialize PROJECT_CONTEXT.md.
e) Verify the project compiles with `mvn clean compile` or `mvn test`.

Then stop, explain how to run and verify Step 1, and propose Step 2 (Core Entities & JPA Repositories) for my approval.
```

---

## Step-by-Step Follow-Up Prompts

Paste one at a time after each step passes and is verified.

### Step 2: Core Entities & Repositories
> "Implement all JPA entities per TRD Section 4: User, Role, Wallet, Voucher, VoucherStatus, Transaction, TransactionType, Route, Trip, Booking, BookingStatus, Ticket, and Ride. Include `@Version` on Trip, Voucher, and Wallet. Implement standard Spring Data JPA repositories with custom query methods for voucher lookups, trip searches, and transaction history. Add unit tests verifying entity mappings and auditing timestamps. Stop after tests pass."

### Step 3: Security, Authentication & User Management
> "Implement Spring Security configuration with BCrypt password hashing, session-based login for Thymeleaf, and HTTP Basic authentication for `/api/**`. Implement UserService and custom UserDetailsService. Create a DataInitializer (CommandLineRunner) to auto-seed a default Admin (admin@mobility.com / admin123) and a sample User (user@mobility.com / user123) with active wallets and test routes. Add tests for password encryption and role authorization. Stop after tests pass."

### Step 4: Wallet, Voucher Engine & QR Code Service
> "Implement WalletService, VoucherService, and QrCodeService (using ZXing). Support single voucher creation, bulk batch generation, assignment to users, and automated expiry validation. Ensure QR code generation produces valid Base64 data URIs. Write comprehensive JUnit 5 tests verifying voucher lifecycle transitions (CREATED -> ASSIGNED -> EXPIRED) and wallet ledger entries. Stop after tests pass."

### Step 5: Taxi Ride QR Redemption & Double-Redeem Concurrency Test
> "Implement RideService for taxi ride redemption per TRD Section 5.1. Ensure redemption is atomic (@Transactional), validates voucher status, records the Ride, and updates the Voucher with optimistic locking. Write `VoucherDoubleRedeemConcurrencyTest` using ExecutorService and CountDownLatch with 10 parallel threads attempting to redeem the exact same voucher simultaneously. Confirm exactly 1 succeeds and 9 fail with an optimistic locking exception. Stop after tests pass."

### Step 6: Transit Engine, Seat Booking & Overbooking Concurrency Test
> "Implement TransitService for trip search, seat booking, and booking cancellation with 100% wallet refund. Implement the optimistic locking retry mechanism for seat reservations per TRD Section 5.2. Write `SeatOverbookingConcurrencyTest` simulating 10 concurrent users attempting to book the last available seat on a trip. Verify zero overbooking. Write tests for booking cancellation and automatic wallet refund. Stop after tests pass."

### Step 7: Thymeleaf Web UI & Admin Analytics
> "Build the complete server-rendered Thymeleaf interface with plain, modern CSS:
> 1. Public/Auth pages: Login, Register with clean error messages.
> 2. User Portal: Wallet balance card, assigned voucher cards with QR display modal, trip search and seat booking page, 'My Tickets' with verification QR, and paginated transaction ledger.
> 3. Admin Portal: Voucher creation and bulk generation form, trip scheduling form, live ride redemption audit table, and an analytics dashboard with Chart.js showing daily spend, route revenues, and seat load factors.
> Test all views in the browser and confirm zero broken styles."

### Step 8: REST API, Swagger Documentation & Final Polish
> "Expose all core services via REST controllers under `/api/**` with DTO validation and GlobalExceptionHandler (@ControllerAdvice). Configure SpringDoc OpenAPI annotations for Swagger UI (/swagger-ui.html). Write a comprehensive README.md with architecture diagrams, setup instructions for MySQL Workbench, sample cURL commands, and a technical interview preparation guide explaining optimistic locking, ACID transactions, and financial data modeling."

---

## Technical Interview Preparation Notes (Keep in Mind)

When explaining this project in an interview:
1. **Why `@Version` (Optimistic Locking) over `Pessimistic Locking`?**
   - Pessimistic locking (`SELECT ... FOR UPDATE`) holds database row locks, which degrades throughput under read-heavy transit searches.
   - Optimistic locking assumes collisions are rare and verifies the version number at update time (`WHERE id = ? AND version = ?`), ensuring non-blocking reads and high concurrency.
2. **Why `BigDecimal` instead of `double` or `float`?**
   - Binary floating-point numbers cannot represent decimal fractions (like `0.10`) accurately due to IEEE 754 precision errors. `BigDecimal` provides arbitrary-precision signed decimal numbers, preventing rounding leakage in financial ledgers.
3. **How is the Ledger Immutable?**
   - The `Transaction` table is strictly append-only. There are no `UPDATE` or `DELETE` operations on transaction records; refunds create new `CREDIT` entries referencing the cancelled booking.
