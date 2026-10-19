package com.banco.ms_cuentas.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.banco.ms_cuentas.model.AccountMovement;

public interface AccountMovementRepository extends CrudRepository<AccountMovement, Long>{

    Optional<AccountMovement> findByIdempotencyKey(String idempotencyKey);

}
