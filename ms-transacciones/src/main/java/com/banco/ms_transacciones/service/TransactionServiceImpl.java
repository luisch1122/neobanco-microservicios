package com.banco.ms_transacciones.service;


import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.banco.ms_transacciones.client.AccountClient;
import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.model.Transaction;
import com.banco.ms_transacciones.model.Transaction.EstadoTransaccion;
import com.banco.ms_transacciones.repository.TransactionRepository;


@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
            AccountClient accountClient){
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    

    @Override
    public Transaction transfer(TransactionRequest transactionRequest, String auth) {

        Optional<Transaction> key = transactionRepository
            .findByIdempotencyKey( 
                transactionRequest.getIdempotencyKey()
            );

        if(key.isPresent()){

            return key.get();
            
        }
    
        if(transactionRequest.getCuentaOrigen().equals(transactionRequest.getCuentaDestino())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Misma cuenta de destino y de origen");
        }

        Transaction transaction = new Transaction();

        transaction.setIdempotencyKey(transactionRequest.getIdempotencyKey());
        transaction.setCuentaOrigen(transactionRequest.getCuentaOrigen());
        transaction.setCuentaDestino(transactionRequest.getCuentaDestino());
        transaction.setMonto(transactionRequest.getMonto());
        
        transactionRepository.save(transaction);

        try {

            accountClient.debit(
                transactionRequest.getCuentaOrigen(), 
                transactionRequest.getMonto(), 
                auth
            );

        } catch (RestClientException ex) {

            transaction.setEstado(EstadoTransaccion.FALLIDA);
            transaction.setMotivoFallo(getErrorMessage(ex));

            transactionRepository.save(transaction);

            throw new ResponseStatusException(
                getErrorStatus(ex),
                getErrorMessage(ex),
                ex
            );
        }

        try {

            accountClient.credit(
                transactionRequest.getCuentaDestino(),
                transactionRequest.getMonto(),
                auth
            );

        } catch (RestClientException ex) {
            
            try {

            accountClient.credit(
                transactionRequest.getCuentaOrigen(), 
                transactionRequest.getMonto(),
                auth
            );

            transaction.setEstado(EstadoTransaccion.REVERTIDA);
            transaction.setMotivoFallo(getErrorMessage(ex));
            
            transactionRepository.save(transaction);

            } catch (RestClientException e) {

                transaction.setEstado(EstadoTransaccion.FALLO_CRITICO_SIN_COMPENSAR);
                transaction.setMotivoFallo(getErrorMessage(e));

            
                transactionRepository.save(transaction);

                throw new ResponseStatusException(
                    getErrorStatus(e),
                    getErrorMessage(e),
                    e
                );

            }

            throw new ResponseStatusException(
                getErrorStatus(ex),
                getErrorMessage(ex),
                ex
            );
        }

        transaction.setEstado(EstadoTransaccion.COMPLETADA);

        transactionRepository.save(transaction);
    
        return transaction;
    }

    @Override
    public List<Transaction> list(String numeroCuenta, String auth) {
        
        try {
            
            accountClient.verify(numeroCuenta, auth);

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    getErrorStatus(e),
                    getErrorMessage(e),
                    e
                );

        }

        return transactionRepository.findByCuentaOrigenOrCuentaDestino(numeroCuenta, numeroCuenta);

    }

    @Override
    public Transaction detail(Long id, String auth) {
        
        Optional<Transaction> trx = transactionRepository.findById(id);
        
        if (trx.isPresent()) {

            Transaction transaction = trx.get();

            try {
                
                accountClient.verify(
                    transaction.getCuentaDestino(), 
                    auth);

                return transaction;
                
            } catch (RestClientException e) {
                
                try {

                    accountClient.verify(
                        transaction.getCuentaOrigen(), 
                        auth);
                    
                    return transaction;
                    

                } catch (RestClientException ex) {

                    throw new ResponseStatusException(
                        getErrorStatus(ex),
                        getErrorMessage(ex),
                        ex
                    );
                  }
            }

        } else {
            
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transacción no encontrada");

        }

    }

    private String getErrorMessage(RestClientException ex) {
        if (ex instanceof RestClientResponseException) {
            return ((RestClientResponseException) ex).getResponseBodyAsString();
        }
        return "No fue posible conectar con el servicio de cuentas";
    }

    private HttpStatusCode getErrorStatus(RestClientException ex) {
        if (ex instanceof RestClientResponseException) {
            return ((RestClientResponseException) ex).getStatusCode();
        }
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

}
