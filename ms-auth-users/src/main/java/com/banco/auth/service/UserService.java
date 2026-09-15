package com.banco.auth.service;

import com.banco.auth.model.Users;

import java.util.Optional;

public interface UserService {

    Users save(Users users);

    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

}
