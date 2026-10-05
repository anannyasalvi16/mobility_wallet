package com.mobilitywallet.service.impl;

import com.mobilitywallet.dto.request.RegistrationDto;
import com.mobilitywallet.entity.Role;
import com.mobilitywallet.entity.User;
import com.mobilitywallet.entity.Wallet;
import com.mobilitywallet.exception.DuplicateEmailException;
import com.mobilitywallet.exception.ResourceNotFoundException;
import com.mobilitywallet.repository.UserRepository;
import com.mobilitywallet.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegistrationDto dto) {
        String cleanEmail = dto.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new DuplicateEmailException("An account with email " + dto.getEmail() + " already exists.");
        }

        User user = new User();
        user.setName(dto.getName().trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setRole(Role.ROLE_USER); // Always ROLE_USER, never accepts role from input
        user.setEnabled(true);

        // Auto-create wallet with balance 0.00 linked to user (commits or rolls back together via cascade)
        Wallet wallet = new Wallet(user, BigDecimal.ZERO.setScale(2));
        user.setWallet(wallet);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
