package com.banco.ms_transacciones.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.model.Transaction;


@Service
public class TransactionServiceImpl implements TransactionService {

    @Override
    public Transaction transfer(TransactionRequest transactionRequest, String auth, Long userId) {
    
    
        return null;
    }
    @Override
    public List<Transaction> listar(Long userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listar'");
    }

    @Override
    public Transaction detalle(Long id, Long userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'detalle'");
    }

}
