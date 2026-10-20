package com.banco.ms_transacciones.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.banco.ms_transacciones.client.AccountClient;
import com.banco.ms_transacciones.client.MovementStatus;
import com.banco.ms_transacciones.config.SystemTokenProvider;
import com.banco.ms_transacciones.model.Transaction;
import com.banco.ms_transacciones.model.Transaction.EstadoTransaccion;
import com.banco.ms_transacciones.model.Transaction.PasoSaga;
import com.banco.ms_transacciones.repository.TransactionRepository;
import com.banco.ms_transacciones.util.RestClientErrorUtils;

@Service
public class ReconciliationService {

    /**
     * Límite de reintentos antes de dejar de insistir automáticamente: pasado
     * este número, la transacción sigue en su estado actual a la espera de
     * revisión manual, en vez de reintentar indefinidamente.
     */
    private static final int MAX_INTENTOS = 5;

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final SystemTokenProvider systemTokenProvider;

    public ReconciliationService(TransactionRepository transactionRepository,
            AccountClient accountClient,
            SystemTokenProvider systemTokenProvider) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
        this.systemTokenProvider = systemTokenProvider;
    }

    @Scheduled(fixedDelayString = "${reconciliacion.intervalo-ms:7200000}")
    public void conciliarProgramado() {
        conciliar();
    }

    /**
     * Punto de entrada único, usado tanto por el job programado como por el
     * endpoint manual de conciliación.
     */
    public void conciliar() {

        String auth = systemTokenProvider.generarTokenSistema();

        List<Transaction> pendientes = transactionRepository.findByEstadoIn(
            List.of(EstadoTransaccion.PENDIENTE_VERIFICACION, EstadoTransaccion.FALLO_CRITICO_SIN_COMPENSAR)
        );

        for (Transaction transaction : pendientes) {

            try {
                conciliarTransaccion(transaction, auth);
            } catch (Exception ex) {
                // Una transacción problemática no debe interrumpir la conciliación del resto del lote.
            }

        }
    }

    private void conciliarTransaccion(Transaction transaction, String auth) {

        if (transaction.getIntentosConciliacion() >= MAX_INTENTOS) {
            // Ya se intentó suficientes veces de forma automática: requiere revisión manual.
            return;
        }

        transaction.setIntentosConciliacion(transaction.getIntentosConciliacion() + 1);

        PasoSaga paso = transaction.getPasoPendiente();

        if (paso == null) {
            // Estado crítico heredado sin paso explícito: lo más seguro es intentar compensar.
            paso = PasoSaga.COMPENSACION_ORIGEN;
        }

        switch (paso) {
            case DEBITO_ORIGEN -> conciliarDebito(transaction, auth);
            case CREDITO_DESTINO -> conciliarCredito(transaction, auth);
            case COMPENSACION_ORIGEN -> conciliarCompensacion(transaction, auth);
        }
    }

    private void conciliarDebito(Transaction transaction, String auth) {

        MovementStatus status = accountClient.verifyMovement(transaction.getIdempotencyKey() + ":DEBITO", auth);

        switch (status) {
            case EXISTE -> intentarCredito(transaction, auth);
            case NO_EXISTE -> {
                transaction.setEstado(EstadoTransaccion.FALLIDA);
                transaction.setPasoPendiente(null);
                transaction.setMotivoFallo("Débito no confirmado tras conciliación: la operación no se ejecutó");
                transactionRepository.save(transaction);
            }
            case DESCONOCIDO -> transactionRepository.save(transaction);
        }
    }

    private void conciliarCredito(Transaction transaction, String auth) {

        MovementStatus status = accountClient.verifyMovement(transaction.getIdempotencyKey() + ":CREDITO", auth);

        switch (status) {
            case EXISTE -> {
                transaction.setEstado(EstadoTransaccion.COMPLETADA);
                transaction.setPasoPendiente(null);
                transactionRepository.save(transaction);
            }
            // El crédito nunca llegó a destino: por seguridad se prefiere compensar
            // (devolver el dinero a origen) en vez de reintentar el crédito original.
            case NO_EXISTE -> intentarCompensacion(transaction, auth);
            case DESCONOCIDO -> transactionRepository.save(transaction);
        }
    }

    private void conciliarCompensacion(Transaction transaction, String auth) {

        MovementStatus status = accountClient.verifyMovement(transaction.getIdempotencyKey() + ":COMPENSACION", auth);

        switch (status) {
            case EXISTE -> {
                transaction.setEstado(EstadoTransaccion.REVERTIDA);
                transaction.setPasoPendiente(null);
                transactionRepository.save(transaction);
            }
            case NO_EXISTE -> intentarCompensacion(transaction, auth);
            case DESCONOCIDO -> transactionRepository.save(transaction);
        }
    }

    private void intentarCredito(Transaction transaction, String auth) {

        try {

            accountClient.credit(
                transaction.getCuentaDestino(),
                transaction.getMonto(),
                auth,
                transaction.getIdempotencyKey() + ":CREDITO"
            );

            transaction.setEstado(EstadoTransaccion.COMPLETADA);
            transaction.setPasoPendiente(null);
            transactionRepository.save(transaction);

        } catch (RestClientException ex) {
            // Si tampoco se puede confirmar el crédito ahora, se prioriza compensar.
            intentarCompensacion(transaction, auth);
        }
    }

    private void intentarCompensacion(Transaction transaction, String auth) {

        try {

            accountClient.credit(
                transaction.getCuentaOrigen(),
                transaction.getMonto(),
                auth,
                transaction.getIdempotencyKey() + ":COMPENSACION"
            );

            transaction.setEstado(EstadoTransaccion.REVERTIDA);
            transaction.setPasoPendiente(null);

        } catch (RestClientException ex) {

            transaction.setEstado(EstadoTransaccion.FALLO_CRITICO_SIN_COMPENSAR);
            transaction.setPasoPendiente(PasoSaga.COMPENSACION_ORIGEN);
            transaction.setMotivoFallo(RestClientErrorUtils.getErrorMessage(ex));
        }

        transactionRepository.save(transaction);
    }

}
