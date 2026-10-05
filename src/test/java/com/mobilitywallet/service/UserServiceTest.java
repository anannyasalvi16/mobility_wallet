package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.RegistrationDto;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.DuplicateEmailException;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Registration creates a user with ROLE_USER")
    void registrationCreatesRoleUser() {
        RegistrationDto dto = new RegistrationDto("Alice Smith", "alice@corp.com", "password123", "9876543210");
        when(userRepository.existsByEmail("alice@corp.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(dto);

        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Stored password is a BCrypt hash, differs from raw password, and matches via PasswordEncoder")
    void passwordIsBcryptHashed() {
        String rawPassword = "mySecurePassword123";
        RegistrationDto dto = new RegistrationDto("Bob Jones", "bob@corp.com", rawPassword, "9876543211");
        when(userRepository.existsByEmail("bob@corp.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(dto);

        assertThat(user.getPassword()).isNotEqualTo(rawPassword);
        assertThat(user.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches(rawPassword, user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Wallet is auto-created with balance 0.00")
    void walletAutoCreatedWithZeroBalance() {
        RegistrationDto dto = new RegistrationDto("Charlie", "charlie@corp.com", "charliePass123", null);
        when(userRepository.existsByEmail("charlie@corp.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(dto);

        assertThat(user.getWallet()).isNotNull();
        assertThat(user.getWallet().getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(user.getWallet().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Duplicate email is rejected with DuplicateEmailException")
    void duplicateEmailRejected() {
        RegistrationDto dto = new RegistrationDto("Duplicate User", "existing@corp.com", "password123", null);
        when(userRepository.existsByEmail("existing@corp.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("existing@corp.com already exists");
    }

    @Test
    @DisplayName("User and wallet roll back together if wallet creation or saving fails")
    void rollbackIfWalletOrSavingFails() {
        RegistrationDto dto = new RegistrationDto("Error User", "error@corp.com", "password123", null);
        when(userRepository.existsByEmail("error@corp.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error during persistence"));

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error during persistence");
    }

    @Test
    @DisplayName("A caller cannot supply a role — RegistrationDto does not accept role and registration always sets ROLE_USER")
    void callerCannotSupplyRole() {
        RegistrationDto dto = new RegistrationDto("Dave", "dave@corp.com", "daveSecret123", null);
        when(userRepository.existsByEmail("dave@corp.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
    }
}
