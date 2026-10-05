# Mobility Wallet — Product Requirements Document (PRD)

**Project Name:** Mobility Wallet  
**Version:** 1.0  
**Target Platform:** Web (Spring Boot 3 + Thymeleaf + MySQL)  
**Author / Engineering Lead:** Senior Java Pair-Programmer & B.E. IT Engineering Student  

---

## 1. Executive Summary & Problem Statement

Organizations (corporations, universities, non-profits, and event organizers) routinely provide travel funds to employees, students, delegates, and guests. Today, this process is predominantly manual:
- **Cash advances** have no spending restrictions or real-time tracking.
- **Reimbursement workflows** require manual receipt collection, invoice verification, and weeks of spreadsheet processing.
- **Misuse & Leakage:** Funds earmarked specifically for local transport are frequently diverted to non-travel expenses.

**Mobility Wallet** is a closed-loop digital prepaid voucher and transit ticketing platform. It guarantees:
1. **Purpose-Bound Spending:** Organization-issued vouchers can strictly be used for taxi rides or bus/metro transit bookings.
2. **Zero Financial Leakage:** Unused vouchers expire automatically and return unused quotas to the organization.
3. **Double-Spend & Overbooking Prevention:** Mathematical concurrency controls ensure vouchers cannot be redeemed twice and transit seats cannot be overbooked.
4. **Complete Rupee Traceability:** Every credit, debit, redemption, and refund is recorded in an immutable ledger.

---

## 2. Personas & User Roles

| Persona | Role Key | Real-World Example | Primary Objectives |
|---|---|---|---|
| **System Admin** | `ROLE_ADMIN` | Campus Transport Manager, HR Operations Lead | Issue single/bulk vouchers, assign to users, manage routes and trip schedules, track utilization analytics. |
| **End User** | `ROLE_USER` | College Student, Corporate Employee, Event Attendee | Hold personal wallet, view assigned vouchers, display voucher QR for taxi rides, search transit schedules, book & cancel tickets, view ledger. |
| **Transit Driver / Conductor** | Operator (via Admin or API) | Taxi Driver, Bus Conductor | Validate and redeem voucher QRs for rides, scan and validate transit ticket QRs. |

---

## 3. Goals & Non-Goals

### 3.1 Primary Goals
- **G1 (Voucher Lifecycle):** Admins can issue vouchers with fixed monetary values, unique codes, and strict expiration dates; assign them to users; and track their status (`CREATED`, `ASSIGNED`, `REDEEMED`, `EXPIRED`).
- **G2 (Taxi Ride QR Redemption):** Users can display a secure QR code for their voucher. Drivers/operators redeem the voucher, instantly generating an immutable ride record.
- **G3 (Transit Search & Booking):** Users can search bus/metro trips by origin, destination, and date, and book available seats using voucher or wallet balance.
- **G4 (Concurrency & Integrity):** Strict prevention of double voucher redemptions and seat overbooking using optimistic locking (`@Version`) and retries.
- **G5 (Automated Refunds):** Cancellations prior to departure automatically release reserved seats and credit 100% of the fare back to the user's digital wallet.
- **G6 (Audit Ledger):** Every rupee transaction produces an immutable `Transaction` record with a timestamp, type (`CREDIT`/`DEBIT`), amount, and description.
- **G7 (Analytics Dashboard):** Real-time admin visibility into issued vs. redeemed volume, daily expenditure velocity, route-wise revenue, and seat occupancy rates.

### 3.2 Non-Goals (Scope for v1)
- No external banking/payment gateway integration (Razorpay/Stripe). Wallet balances and vouchers are organization-managed.
- No live GPS vehicle tracking or turn-by-turn navigation.
- No mobile native apps (iOS/Android); responsive server-rendered web UI optimized for both desktop and mobile browsers.

---

## 4. User Stories

### User Stories (Employee / Student)
- **US-1.1:** As a user, I can register and log in securely so that my travel allowance and wallet are protected.
- **US-1.2:** As a user, I can view my wallet balance and list of assigned active/redeemed/expired vouchers with their validity dates.
- **US-1.3:** As a user, I can open a voucher to view a generated QR code that a taxi driver can scan to settle my travel bill.
- **US-1.4:** As a user, I can search for scheduled bus or metro trips between an origin and destination for a given date.
- **US-1.5:** As a user, I can book one or more seats on a selected trip using my voucher or my wallet balance.
- **US-1.6:** As a user, I can view my booked tickets along with a scannable verification QR code containing seat and booking references.
- **US-1.7:** As a user, I can cancel my booking before trip departure and receive an instant refund credited to my wallet.
- **US-1.8:** As a user, I can view a paginated, filterable transaction ledger showing every credit and debit to my account.

### Admin Stories (HR / Transport Manager)
- **US-2.1:** As an admin, I can create individual vouchers or generate bulk batches with predefined face values and expiry dates.
- **US-2.2:** As an admin, I can assign available vouchers to registered users.
- **US-2.3:** As an admin, I can create transit routes (e.g., Campus to Downtown Metro Station) and schedule specific trips with departure times, seat capacities, and ticket fares.
- **US-2.4:** As an admin, I can view all taxi ride redemptions with driver details, timestamps, and redeemed voucher references.
- **US-2.5:** As an admin, I can view visual analytics (voucher conversion, daily spend, route revenues, seat load factors) to budget future travel allowances accurately.

---

## 5. Detailed Functional Specifications

### 5.1 Authentication & Role-Based Access Control
- User registration captures Name, Email, Password, and Phone Number.
- Passwords hashed using Spring Security `BCryptPasswordEncoder`.
- Roles: `ROLE_USER` and `ROLE_ADMIN`.
- Upon successful registration, a `Wallet` is automatically provisioned for the user with an initial balance of `₹0.00`.
- Dual interfaces: Session-based cookie authentication for the Thymeleaf web portal, and HTTP Basic authentication for the `/api` REST endpoints.

### 5.2 Voucher Engine
- **Attributes:** Code (unique human-readable format `VCH-YYYY-XXXXXX`), Face Value (`BigDecimal`), Expiry Date (`LocalDate`), Status (`CREATED`, `ASSIGNED`, `REDEEMED`, `EXPIRED`), Assigned User, Creator Admin, Redeemed At timestamp, Version (`@Version`).
- **Bulk Creation:** Admin inputs quantity $N$ and face value $V$. The system generates $N$ unique voucher codes in the `CREATED` state.
- **Assignment:** Admin assigns `CREATED` vouchers to specific user IDs, transitioning them to `ASSIGNED`.
- **Expiry Rules:** Vouchers past their expiry date cannot be redeemed. An automated midnight sweep transitions past-due vouchers to `EXPIRED`, backed by lazy validation upon any redemption attempt.
- **Redemption Rules:** 
  - For Taxi Rides: Full-face redemption against a verified ride.
  - For Transit Bookings: A voucher can be selected during checkout. If voucher value exceeds fare, voucher is marked `REDEEMED` and the fare is covered. If voucher value is less than fare, the remaining balance is deducted from the user's wallet (split payment).

### 5.3 Taxi Ride QR Redemption Flow
1. User clicks "Redeem for Taxi" on an active assigned voucher.
2. The system generates a high-resolution QR code encoding a secure redemption payload:
   `{"code":"VCH-2026-ABC123","val":500.00,"uid":42,"sig":"HMAC-SHA256"}`
3. The taxi driver/operator scans the QR or accesses the redemption portal, inputs the ride details (driver name, vehicle number, pickup, drop, meter fare), and clicks "Confirm Redemption".
4. The system validates:
   - Voucher exists and status is `ASSIGNED`.
   - Voucher belongs to the user and is not expired.
5. In an atomic transaction:
   - Voucher status transitions to `REDEEMED`, with `redeemedAt = now()`.
   - A `Ride` record is created linking user, voucher, fare, and vehicle details.
   - An immutable `Transaction` (`DEBIT`) is appended to the audit ledger.
6. If a concurrent attempt hits the same voucher, optimistic locking aborts the second attempt with an explicit `VoucherAlreadyRedeemedException`.

### 5.4 Transit Booking & Overbooking Prevention
1. **Search:** Filter trips by `origin`, `destination`, and `departureDate`.
2. **Seat Selection:** System displays total seats and available seats. Sequential seat allocation assigns deterministic seat numbers (e.g., Seat #12).
3. **Checkout:** User chooses payment source:
   - `WALLET`: Deducts from user's wallet balance.
   - `VOUCHER`: Redeems assigned voucher (with optional wallet top-up for shortfall).
4. **Concurrency Protocol:**
   - The `Trip` row maintains `availableSeats` and a numeric `@Version` column.
   - When user submits booking for $K$ seats, system verifies `availableSeats >= K`.
   - Decrements `availableSeats = availableSeats - K` and saves within `@Transactional`.
   - If another user reserved the seat in parallel, Hibernate detects a version mismatch and throws `OptimisticLockException`.
   - The booking service retries up to 3 times before returning a user-friendly error: *"The selected seat was just reserved by another commuter. Please pick another trip."*
5. **Ticket Issuance:** Generates a `Ticket` with a unique QR code for onboard verification.

### 5.5 Cancellation & Refund Logic
- Commuters can cancel a confirmed booking up to 30 minutes before the scheduled `departureTime`.
- Upon cancellation:
  - `Booking.status` becomes `CANCELLED`.
  - `Ticket.validated` becomes `false` / `CANCELLED`.
  - `Trip.availableSeats` is incremented by the cancelled seat count.
  - 100% of the booking fare is refunded directly into the user's `Wallet`.
  - An immutable `CREDIT` transaction is recorded: *"Refund for cancelled booking #BKG-1002"*.

### 5.6 Admin Analytics & Reporting
- **Voucher Funnel:** Metric cards for Issued, Assigned, Redeemed, and Expired counts and rupee values.
- **Daily Spend Trend:** Chart depicting total mobility funds disbursed vs. consumed per day over the last 30 days.
- **Route Revenue & Load Factor:** Bar chart showing gross bookings per route and percentage seat occupancy ($\frac{\text{bookedSeats}}{\text{totalSeats}} \times 100$).

---

## 6. Success Metrics & Key Performance Indicators (KPIs)

1. **Zero Double-Redemption Rate:** $0\%$ duplicate voucher redemptions under concurrent load tests.
2. **Zero Overbooking Rate:** $0\%$ overbooked seats across simultaneous booking requests on the final available seat.
3. **Transaction Auditability:** $100\%$ reconciliation between wallet/voucher balance movements and transaction ledger rows.
4. **Performance:** Search and booking API response times under $150\text{ ms}$ on standard MySQL 8 indexes.
