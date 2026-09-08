package com.banco.auth.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banco.auth.model.Users;
import com.banco.auth.repository.UsersRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private UsersRepository repository;

    public JpaUserDetailsService(UsersRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<Users> emailOptional = repository.findByEmail(email);
       
        if(emailOptional.isEmpty()){
            throw new UsernameNotFoundException(String.format("No existe el email %s en el sistema", email));
        }

        Users users = emailOptional.orElseThrow();

        return User
            .withUsername(users.getEmail())
            .password(users.getPassword())
            .authorities(java.util.List.of())
            .build();
        
    }

}
