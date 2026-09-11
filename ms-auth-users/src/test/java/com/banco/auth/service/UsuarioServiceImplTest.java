package com.banco.auth.service;

import com.banco.auth.model.Users;
import com.banco.auth.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Test
    void guardarDelegaElUsuarioAlRepositorio() {
        Users usuario = new Users();
        Users usuarioGuardado = new Users();

        when(usersRepository.save(usuario)).thenReturn(usuarioGuardado);

        Users resultado = userServiceImpl.save(usuario);

        assertSame(usuarioGuardado, resultado);
        verify(usersRepository).save(usuario);
    }
}
