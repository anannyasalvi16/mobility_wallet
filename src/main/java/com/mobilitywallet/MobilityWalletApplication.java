package com.mobilitywallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Mobility Wallet application.
 * Manages transit allowance vouchers, wallet ledger, and transit ticket booking.
 */
@SpringBootApplication
public class MobilityWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobilityWalletApplication.class, args);
    }
}
