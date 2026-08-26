package com.banco.auth.service;

import com.banco.auth.model.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registrarEncriptaLaContrasenaYGuardaElUsuario() {
        Users usuario = new Users();
        usuario.setPasswordHash("contrasena-original");
        Users usuarioGuardado = new Users();
        usuarioGuardado.setPasswordHash("hash-generado");

        when(passwordEncoder.encode("contrasena-original")).thenReturn("hash-generado");
        when(userService.save(usuario)).thenReturn(usuarioGuardado);

        Users resultado = authService.register(usuario);

        assertSame(usuarioGuardado, resultado);
        verify(passwordEncoder).encode("contrasena-original");
        verify(userService).save(usuario);
    }
}
