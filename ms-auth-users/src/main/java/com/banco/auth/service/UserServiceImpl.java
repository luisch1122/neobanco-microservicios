package com.banco.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banco.auth.model.Users;
import com.banco.auth.repository.UsersRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;

    public UserServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    @Transactional
    public Users save(Users users) {
        return usersRepository.save(users);
    }

    @Override
    public boolean existsByEmail(String email) {
        return  usersRepository.existsByEmail(email);
    }

}
