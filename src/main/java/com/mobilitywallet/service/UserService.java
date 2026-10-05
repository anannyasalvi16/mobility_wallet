package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.RegistrationDto;
import com.mobilitywallet.entity.User;

import java.util.List;

public interface UserService {

    User register(RegistrationDto dto);

    User findByEmail(String email);

    User findById(Long id);

    boolean existsByEmail(String email);

    List<User> findAllUsers();
}
