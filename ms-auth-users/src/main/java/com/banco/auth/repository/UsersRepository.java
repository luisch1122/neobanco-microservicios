package com.banco.auth.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.banco.auth.model.Users;


public interface UsersRepository extends CrudRepository<Users, Long> {


    boolean existsByEmail(String email);


    Optional<Users> findByEmail(String email);
    
} 

