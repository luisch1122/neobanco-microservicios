package com.banco.ms_transacciones.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.banco.ms_transacciones.model.Transaction;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByCuentaOrigenOrCuentaDestino(String cuentaOrigen, String cuentaDestino);

    Optional<Transaction> findById(Long id);

}
