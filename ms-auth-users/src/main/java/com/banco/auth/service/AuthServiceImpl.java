package com.banco.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.banco.auth.model.Users;

import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserService usuarioService, PasswordEncoder passwordEncoder) {
        this.userService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Users register(Users users) {

        users.setPasswordHash(
            passwordEncoder.encode(users.getPasswordHash())
        );
        
        return userService.save(users);
    }

    @Override
    public String login(String username, String password) {
        // Implement the logic to authenticate a user and return a JWT token
        return null;
    }

}
