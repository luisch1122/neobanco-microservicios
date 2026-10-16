package com.banco.ms_transacciones.service;

import java.util.List;

import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.model.Transaction;

public interface TransactionService {

    Transaction transfer(TransactionRequest transactionRequest, String auth);

    List<Transaction> list(String numeroCuenta, String auth);

    Transaction detail(Long id, String auth);

}
