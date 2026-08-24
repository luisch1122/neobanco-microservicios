package com.banco.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.banco.auth.model.Usuario;

import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private UsuarioService usuarioService;
    private PasswordEncoder passwordEncoder;


    @Override
    public Usuario registrar(Usuario usuario) {
        usuario.setPasswordHash(
            passwordEncoder.encode(usuario.getPasswordHash())
        );
        return usuarioService.guardar(usuario);
    }

    @Override
    public String login(String username, String password) {
        // Implement the logic to authenticate a user and return a JWT token
        return null;
    }

}
