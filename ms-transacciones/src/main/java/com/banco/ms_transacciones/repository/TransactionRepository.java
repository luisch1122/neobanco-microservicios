package com.banco.ms_transacciones.repository;

import org.springframework.data.repository.CrudRepository;

import com.banco.ms_transacciones.model.Transaction;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    

}
