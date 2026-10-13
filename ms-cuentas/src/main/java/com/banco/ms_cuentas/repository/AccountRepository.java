package com.banco.ms_cuentas.repository;

import java.util.List;


import org.springframework.data.repository.CrudRepository;

import com.banco.ms_cuentas.model.Account;

public interface AccountRepository extends CrudRepository<Account, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Account> findByUserId(Long userId);

}
