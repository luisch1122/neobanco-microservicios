package com.banco.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banco.auth.model.Users;
import com.banco.auth.repository.UsersRepository;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    @Transactional
    public Users save(Users users) {
        return usersRepository.save(users);
    }

}
