package com.banco.ms_cuentas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.banco.ms_cuentas.model.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends CrudRepository<Account, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Account> findByUserId(Long userId);

    Account findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.numeroCuenta = :numeroCuenta")
    Account findByNumeroCuentaForUpdate(@Param("numeroCuenta") String numeroCuenta);

    

}
