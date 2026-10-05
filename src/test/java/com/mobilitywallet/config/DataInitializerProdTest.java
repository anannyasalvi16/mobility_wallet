package com.mobilitywallet.config;

import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.repository.RouteRepository;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.repository.VoucherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        // H2 in-memory DB for test isolation while prod profile is active
        "DB_URL=jdbc:h2:mem:prodtestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Valid custom production admin credentials
        "ADMIN_EMAIL=customadmin@prod.com",
        "ADMIN_PASSWORD=ProductionSecretPass999!",
        "ADMIN_NAME=Enterprise Ops Admin",
        "ADMIN_PHONE=9123456789"
})
class DataInitializerProdTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("In prod profile: seeds ONLY admin from env vars; never creates demo user or mock data")
    void prodProfileSeedsOnlyCustomAdmin() {
        // 1. Verify production admin was seeded
        Optional<User> adminOpt = userRepository.findByEmail("customadmin@prod.com");
        assertThat(adminOpt).isPresent();
        User admin = adminOpt.get();
        assertThat(admin.getName()).isEqualTo("Enterprise Ops Admin");
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches("ProductionSecretPass999!", admin.getPassword())).isTrue();

        // 2. Verify dev demo user is NOT seeded in prod
        assertThat(userRepository.existsByEmail("user@mobility.com")).isFalse();

        // 3. Verify dev demo admin (admin@mobility.com) is NOT seeded
        assertThat(userRepository.existsByEmail("admin@mobility.com")).isFalse();

        // 4. Verify no mock vouchers or routes were seeded
        assertThat(voucherRepository.count()).isEqualTo(0);
        assertThat(routeRepository.count()).isEqualTo(0);
    }
}
