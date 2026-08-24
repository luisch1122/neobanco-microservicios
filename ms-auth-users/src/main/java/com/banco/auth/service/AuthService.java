package com.banco.auth.service;
import com.banco.auth.model.Usuario;
public interface AuthService{

    Usuario registrar(Usuario usuario);
    String login(String username, String password);

}
