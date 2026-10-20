package com.banco.ms_transacciones.service;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.banco.ms_transacciones.client.AccountClient;
import com.banco.ms_transacciones.client.MovementStatus;
import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.model.Transaction;
import com.banco.ms_transacciones.model.Transaction.EstadoTransaccion;
import com.banco.ms_transacciones.model.Transaction.PasoSaga;
import com.banco.ms_transacciones.repository.TransactionRepository;
import com.banco.ms_transacciones.util.RestClientErrorUtils;


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

        Optional<Transaction> existing = transactionRepository
            .findByIdempotencyKey( 
                transactionRequest.getIdempotencyKey()
            );

        if(existing.isPresent()){

            Transaction previous = existing.get();

            boolean sameData =
                    Objects.equals(previous.getCuentaOrigen(), transactionRequest.getCuentaOrigen())
                    && Objects.equals(previous.getCuentaDestino(), transactionRequest.getCuentaDestino())
                    && previous.getMonto() != null
                    && transactionRequest.getMonto() != null
                    && previous.getMonto().compareTo(transactionRequest.getMonto()) == 0;            

            if (!sameData) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave de idempotencia ya fue usada con otros datos");
            }

            return previous;
            
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

        String keyDebito = transactionRequest.getIdempotencyKey() + ":DEBITO";

        ResultadoPaso debito = ejecutarPaso(
            () -> accountClient.debit(
                transactionRequest.getCuentaOrigen(),
                transactionRequest.getMonto(),
                auth,
                keyDebito
            ),
            keyDebito,
            auth
        );

        if (debito.resultado() == ResultadoPaso.Resultado.AMBIGUA) {

            transaction.setEstado(EstadoTransaccion.PENDIENTE_VERIFICACION);
            transaction.setPasoPendiente(PasoSaga.DEBITO_ORIGEN);
            transaction.setMotivoFallo(debito.mensaje());

            transactionRepository.save(transaction);

            // No se lanza excepción: no sabemos si el dinero ya salió de la
            // cuenta de origen. La conciliación resolverá el estado real.
            return transaction;
        }

        if (debito.resultado() == ResultadoPaso.Resultado.FALLO) {

            transaction.setEstado(EstadoTransaccion.FALLIDA);
            transaction.setMotivoFallo(debito.mensaje());

            transactionRepository.save(transaction);

            throw new ResponseStatusException(debito.status(), debito.mensaje());
        }

        String keyCredito = transactionRequest.getIdempotencyKey() + ":CREDITO";

        ResultadoPaso credito = ejecutarPaso(
            () -> accountClient.credit(
                transactionRequest.getCuentaDestino(),
                transactionRequest.getMonto(),
                auth,
                keyCredito
            ),
            keyCredito,
            auth
        );

        if (credito.resultado() == ResultadoPaso.Resultado.EXITO) {

            transaction.setEstado(EstadoTransaccion.COMPLETADA);

            transactionRepository.save(transaction);

            return transaction;
        }

        if (credito.resultado() == ResultadoPaso.Resultado.AMBIGUA) {

            transaction.setEstado(EstadoTransaccion.PENDIENTE_VERIFICACION);
            transaction.setPasoPendiente(PasoSaga.CREDITO_DESTINO);
            transaction.setMotivoFallo(credito.mensaje());

            transactionRepository.save(transaction);

            // No se compensa todavía: no sabemos si el crédito a destino ya
            // se aplicó. La conciliación lo confirmará antes de decidir.
            return transaction;
        }

        // El crédito a destino falló de forma definitiva -> compensar el débito de origen.
        String keyCompensacion = transactionRequest.getIdempotencyKey() + ":COMPENSACION";

        ResultadoPaso compensacion = ejecutarPaso(
            () -> accountClient.credit(
                transactionRequest.getCuentaOrigen(),
                transactionRequest.getMonto(),
                auth,
                keyCompensacion
            ),
            keyCompensacion,
            auth
        );

        if (compensacion.resultado() == ResultadoPaso.Resultado.EXITO) {

            transaction.setEstado(EstadoTransaccion.REVERTIDA);
            transaction.setMotivoFallo(credito.mensaje());

            transactionRepository.save(transaction);

            throw new ResponseStatusException(credito.status(), credito.mensaje());
        }

        if (compensacion.resultado() == ResultadoPaso.Resultado.AMBIGUA) {

            transaction.setEstado(EstadoTransaccion.PENDIENTE_VERIFICACION);
            transaction.setPasoPendiente(PasoSaga.COMPENSACION_ORIGEN);
            transaction.setMotivoFallo(compensacion.mensaje());

            transactionRepository.save(transaction);

            return transaction;
        }

        // Ni el crédito ni la compensación se confirmaron: estado crítico,
        // requiere que la conciliación lo siga reintentando.
        transaction.setEstado(EstadoTransaccion.FALLO_CRITICO_SIN_COMPENSAR);
        transaction.setPasoPendiente(PasoSaga.COMPENSACION_ORIGEN);
        transaction.setMotivoFallo(compensacion.mensaje());

        transactionRepository.save(transaction);

        throw new ResponseStatusException(compensacion.status(), compensacion.mensaje());
    }

    @Override
    public List<Transaction> list(String numeroCuenta, String auth) {
        
        try {
            
            accountClient.verify(numeroCuenta, auth);

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    RestClientErrorUtils.getErrorStatus(e),
                    RestClientErrorUtils.getErrorMessage(e),
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
                        RestClientErrorUtils.getErrorStatus(ex),
                        RestClientErrorUtils.getErrorMessage(ex),
                        ex
                    );
                  }
            }

        } else {
            
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transacción no encontrada");

        }

    }

    /**
     * Ejecuta una llamada a ms-cuentas (débito/crédito/compensación) y
     * clasifica el resultado. Si la llamada falla sin respuesta HTTP
     * explícita (timeout, conexión rechazada), no se asume fallo: se
     * verifica de inmediato contra ms-cuentas si el movimiento realmente se
     * aplicó, para no marcar como fallida una operación que en realidad sí
     * tuvo éxito (recuperación de operaciones ambiguas "en línea").
     */
    private ResultadoPaso ejecutarPaso(Runnable accion, String idempotencyKeyPaso, String auth) {

        try {

            accion.run();
            return ResultadoPaso.exito();

        } catch (RestClientException ex) {

            if (!RestClientErrorUtils.esAmbigua(ex)) {
                return ResultadoPaso.fallo(
                    RestClientErrorUtils.getErrorStatus(ex),
                    RestClientErrorUtils.getErrorMessage(ex)
                );
            }

            MovementStatus status;

            try {
                status = accountClient.verifyMovement(idempotencyKeyPaso, auth);
            } catch (RestClientException verificacionEx) {
                return ResultadoPaso.ambigua(RestClientErrorUtils.getErrorMessage(ex));
            }

            return switch (status) {
                case EXISTE -> ResultadoPaso.exito();
                case NO_EXISTE -> ResultadoPaso.fallo(
                    RestClientErrorUtils.getErrorStatus(ex),
                    RestClientErrorUtils.getErrorMessage(ex)
                );
                case DESCONOCIDO -> ResultadoPaso.ambigua(RestClientErrorUtils.getErrorMessage(ex));
            };
        }
    }

}
