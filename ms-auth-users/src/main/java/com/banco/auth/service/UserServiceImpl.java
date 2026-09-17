package com.banco.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banco.auth.model.Users;
import com.banco.auth.model.UsersRequest;
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

        return usersRepository.existsByEmail(email);

    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Users> findByEmail(String email) {

        return usersRepository.findByEmail(email);

    }

    @Override 
    @Transactional
    public Optional<Users> update(UsersRequest users, String email) throws IllegalArgumentException {

        Optional<Users> userOptional = usersRepository.findByEmail(email);


        if(userOptional.isPresent()) {

            Users userDB = userOptional.get();

            boolean emailChanged = !userDB.getEmail().equalsIgnoreCase(users.getEmail());
            if (emailChanged && usersRepository.existsByEmail(users.getEmail())) {
                throw new IllegalArgumentException("El email ya está en uso por otro usuario");
            }

            userDB.setEmail(users.getEmail());
            userDB.setNombre(users.getNombre());

            return Optional.of(usersRepository.save(userDB));
        }

        return Optional.empty();

    }

}
