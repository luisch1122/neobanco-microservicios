package com.banco.auth.service;
import java.util.Optional;

import com.banco.auth.dto.PasswordRequest;
import com.banco.auth.model.Users;
public interface AuthService{

    Users register(Users users);

    Optional<Users> updatePassword(PasswordRequest pass, String email);

}
