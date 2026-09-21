package com.banco.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.banco.auth.dto.PasswordRequest;
import com.banco.auth.model.Users;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {


    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserService usuarioService, 
                            PasswordEncoder passwordEncoder) {
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

    @Override 
    @Transactional 
    public Optional<Users> updatePassword(PasswordRequest pass, String email){

        Optional<Users> userOptional = userService.findByEmail(email);

        if(userOptional.isPresent()){

            String currentPassword = pass.getCurrentPassword();
            String newPassword = pass.getNewPassword();

            Users userDB = userOptional.get();

            if (!passwordEncoder.matches(currentPassword, userDB.getPassword())) {
                throw new IllegalArgumentException("La contraseña actual no es correcta");
            }

            if (currentPassword.equals(newPassword)) {
                throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
            }

            userDB.setPassword(passwordEncoder.encode(newPassword));

            return Optional.of(userService.save(userDB));
        }
        
        return Optional.empty();

    }
}
