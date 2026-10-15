package com.banco.ms_transacciones.service;

import java.util.List;

import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.model.Transaction;

public interface TransactionService {

    Transaction transfer(TransactionRequest transactionRequest, String auth, Long userId);

    List<Transaction> listar(Long userId);

    Transaction detalle(Long id, Long userId);

}
