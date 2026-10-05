package com.mobilitywallet;

import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.TripRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
public class MySQLSchemaInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Test
    @DisplayName("Verify Hibernate connects to MySQL, creates schema and seeds data")
    void verifyMySQLSchemaAndSeedData() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(walletRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(routeRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(tripRepository.count()).isGreaterThanOrEqualTo(2);
        System.out.println(">>> MySQL Schema and Seed Data successfully initialized and verified!");
    }
}
