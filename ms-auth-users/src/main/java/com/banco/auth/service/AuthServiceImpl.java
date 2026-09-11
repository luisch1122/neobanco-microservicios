package com.banco.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.banco.auth.model.Users;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

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

        if (users.getCreadoEn() == null) {
            users.setCreadoEn(LocalDateTime.now());
        }
        if (users.getEstado() == null || users.getEstado().isBlank()) {
            users.setEstado("ACTIVO");
        }
        
        return userService.save(users);
    }
}
