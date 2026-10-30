-- ==============================================================================
-- Flyway Database Migration: V1__init_schema.sql
-- Initial Schema Definition for Mobility Wallet Platform
-- Supported Engines: MySQL 8.0+ / H2 (MySQL Mode)
-- ==============================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    role ENUM('ROLE_USER', 'ROLE_ADMIN') NOT NULL,
    enabled BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Wallets Table
CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    balance DECIMAL(12, 2) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT fk_wallets_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Vouchers Table
CREATE TABLE IF NOT EXISTS vouchers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    initial_value DECIMAL(12, 2) NOT NULL,
    expiry_date DATE NOT NULL,
    status ENUM('CREATED', 'ASSIGNED', 'REDEEMED', 'EXPIRED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    redeemed_at DATETIME(6) DEFAULT NULL,
    version BIGINT NOT NULL,
    assigned_to_id BIGINT DEFAULT NULL,
    created_by_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vouchers_code UNIQUE (code),
    CONSTRAINT fk_vouchers_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id),
    CONSTRAINT fk_vouchers_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Routes Table
CREATE TABLE IF NOT EXISTS routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    distance_km DOUBLE NOT NULL,
    mode ENUM('BUS', 'METRO') NOT NULL,
    active BIT(1) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Trips Table
CREATE TABLE IF NOT EXISTS trips (
    id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    departure_time DATETIME(6) NOT NULL,
    arrival_time DATETIME(6) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    fare DECIMAL(12, 2) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_trips_route_id FOREIGN KEY (route_id) REFERENCES routes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. Bookings Table
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_reference VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    seat_count INT NOT NULL,
    total_fare DECIMAL(12, 2) NOT NULL,
    payment_method ENUM('WALLET', 'VOUCHER', 'SPLIT') NOT NULL,
    status ENUM('CONFIRMED', 'CANCELLED') NOT NULL,
    booking_date DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT fk_bookings_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_trip_id FOREIGN KEY (trip_id) REFERENCES trips (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. Tickets Table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    seat_number VARCHAR(16) NOT NULL,
    ticket_number VARCHAR(32) NOT NULL,
    qr_code_data TEXT NOT NULL,
    validated BIT(1) NOT NULL,
    validated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tickets_ticket_number UNIQUE (ticket_number),
    CONSTRAINT fk_tickets_booking_id FOREIGN KEY (booking_id) REFERENCES bookings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. Rides Table
CREATE TABLE IF NOT EXISTS rides (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    driver_name VARCHAR(100) NOT NULL,
    vehicle_number VARCHAR(32) NOT NULL,
    pickup_location VARCHAR(150) NOT NULL,
    drop_location VARCHAR(150) NOT NULL,
    fare DECIMAL(12, 2) NOT NULL,
    ride_time DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rides_voucher_id UNIQUE (voucher_id),
    CONSTRAINT fk_rides_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_rides_voucher_id FOREIGN KEY (voucher_id) REFERENCES vouchers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    voucher_id BIGINT DEFAULT NULL,
    type ENUM('CREDIT', 'DEBIT') NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_wallet_id FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_transactions_voucher_id FOREIGN KEY (voucher_id) REFERENCES vouchers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
