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

        users.setPassword(
            passwordEncoder.encode(users.getPassword())
        );
        
        return userService.save(users);
    }
}
