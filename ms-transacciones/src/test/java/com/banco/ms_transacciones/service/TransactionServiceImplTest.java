package com.banco.ms_transacciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.banco.ms_transacciones.client.AccountClient;
import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.model.Transaction;
import com.banco.ms_transacciones.model.Transaction.EstadoTransaccion;
import com.banco.ms_transacciones.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private TransactionServiceImpl transactionServiceImpl;

    private static final String AUTH = "Bearer token";

    private TransactionRequest requestDe(String origen, String destino, String monto) {
        TransactionRequest request = new TransactionRequest();
        request.setIdempotencyKey("key-1");
        request.setCuentaOrigen(origen);
        request.setCuentaDestino(destino);
        request.setMonto(new BigDecimal(monto));
        return request;
    }

    // ---------------------------------------------------------------
    // transfer - idempotencia y validaciones basicas
    // ---------------------------------------------------------------

    @Test
    void transfer_conIdempotencyKeyExistente_devuelveLaTransaccionExistente() {
                Transaction existente = requestTransaction("001", "002");
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existente));

        Transaction resultado = transactionServiceImpl.transfer(requestDe("001", "002", "10.00"), AUTH);

        assertSame(existente, resultado);
        verify(accountClient, never()).debit(anyString(), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void transfer_conCuentaOrigenIgualDestino_lanza409() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.transfer(requestDe("001", "001", "10.00"), AUTH));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ---------------------------------------------------------------
    // transfer - fallo en el debito
    // ---------------------------------------------------------------

    @Test
    void transfer_conDebitoFallido_marcaFallidaYPropagaElError() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        doThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Fondos insuficientes", null, null, null))
                .when(accountClient).debit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:DEBITO"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.transfer(requestDe("001", "002", "10.00"), AUTH));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(accountClient, never()).credit(anyString(), any(BigDecimal.class), anyString(), anyString());

        assertEquals(EstadoTransaccion.FALLIDA, capturarUltimoEstadoGuardado());
    }

    @Test
    void transfer_conFalloDeConexionEnElDebito_mapeaA503() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        doThrow(new ResourceAccessException("timeout"))
                .when(accountClient).debit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:DEBITO"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.transfer(requestDe("001", "002", "10.00"), AUTH));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    // ---------------------------------------------------------------
    // transfer - fallo en el credito (compensacion)
    // ---------------------------------------------------------------

    @Test
    void transfer_conCreditoFallidoYCompensacionExitosa_marcaRevertida() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:DEBITO"));
        doThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Cuenta no encontrada", null, null, null))
                .when(accountClient).credit(eq("002"), any(BigDecimal.class), eq(AUTH), eq("key-1:CREDITO"));
        doNothing().when(accountClient).credit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:COMPENSACION"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.transfer(requestDe("001", "002", "10.00"), AUTH));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        // La compensacion (credito de vuelta a cuentaOrigen) debe haberse invocado.
        verify(accountClient).credit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:COMPENSACION"));
        assertEquals(EstadoTransaccion.REVERTIDA, capturarUltimoEstadoGuardado());
    }

    @Test
    void transfer_conCreditoFallidoYCompensacionFallida_marcaFalloCriticoSinCompensar() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:DEBITO"));
        doThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Cuenta no encontrada", null, null, null))
                .when(accountClient).credit(eq("002"), any(BigDecimal.class), eq(AUTH), eq("key-1:CREDITO"));
        doThrow(new ResourceAccessException("ms-cuentas no disponible"))
                .when(accountClient).credit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:COMPENSACION"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.transfer(requestDe("001", "002", "10.00"), AUTH));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals(EstadoTransaccion.FALLO_CRITICO_SIN_COMPENSAR, capturarUltimoEstadoGuardado());
    }

    // ---------------------------------------------------------------
    // transfer - exito completo
    // ---------------------------------------------------------------

    @Test
    void transfer_conTodoExitoso_marcaCompletadaYDevuelveLaTransaccion() {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), eq(AUTH), eq("key-1:DEBITO"));
        doNothing().when(accountClient).credit(eq("002"), any(BigDecimal.class), eq(AUTH), eq("key-1:CREDITO"));

        Transaction resultado = transactionServiceImpl.transfer(requestDe("001", "002", "10.00"), AUTH);

        assertEquals(EstadoTransaccion.COMPLETADA, resultado.getEstado());
        assertEquals("001", resultado.getCuentaOrigen());
        assertEquals("002", resultado.getCuentaDestino());
    }

    // ---------------------------------------------------------------
    // list
    // ---------------------------------------------------------------

    @Test
    void list_conCuentaSinPermiso_propagaElErrorDeVerify() {
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "No autorizado", null, null, null))
                .when(accountClient).verify("001", AUTH);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.list("001", AUTH));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(transactionRepository, never()).findByCuentaOrigenOrCuentaDestino(anyString(), anyString());
    }

    @Test
    void list_conCuentaValida_devuelveLasTransaccionesDeEsaCuenta() {
        doNothing().when(accountClient).verify("001", AUTH);
        when(transactionRepository.findByCuentaOrigenOrCuentaDestino("001", "001"))
                .thenReturn(List.of(new Transaction()));

        List<Transaction> resultado = transactionServiceImpl.list("001", AUTH);

        assertEquals(1, resultado.size());
    }

    // ---------------------------------------------------------------
    // detail
    // ---------------------------------------------------------------

    @Test
    void detail_conTransaccionInexistente_lanza404() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.detail(1L, AUTH));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void detail_conUsuarioDuenoDeCuentaDestino_devuelveLaTransaccion() {
        Transaction transaction = requestTransaction("001", "002");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        doNothing().when(accountClient).verify("002", AUTH);

        Transaction resultado = transactionServiceImpl.detail(1L, AUTH);

        assertSame(transaction, resultado);
    }

    @Test
    void detail_conUsuarioDuenoSoloDeCuentaOrigen_devuelveLaTransaccionPorFallback() {
        Transaction transaction = requestTransaction("001", "002");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "No autorizado", null, null, null))
                .when(accountClient).verify("002", AUTH);
        doNothing().when(accountClient).verify("001", AUTH);

        Transaction resultado = transactionServiceImpl.detail(1L, AUTH);

        assertSame(transaction, resultado);
    }

    @Test
    void detail_conUsuarioSinPermisoEnNingunaCuenta_lanzaElErrorDelSegundoIntento() {
        Transaction transaction = requestTransaction("001", "002");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "No autorizado", null, null, null))
                .when(accountClient).verify("002", AUTH);
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "No autorizado", null, null, null))
                .when(accountClient).verify("001", AUTH);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionServiceImpl.detail(1L, AUTH));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Transaction requestTransaction(String origen, String destino) {
        Transaction transaction = new Transaction();
        transaction.setCuentaOrigen(origen);
        transaction.setCuentaDestino(destino);
        transaction.setMonto(new BigDecimal("10.00"));
        return transaction;
    }

    /** Captura el estado de la ultima invocacion a save() sobre el repositorio. */
    private EstadoTransaccion capturarUltimoEstadoGuardado() {
        org.mockito.ArgumentCaptor<Transaction> captor = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<Transaction> capturadas = captor.getAllValues();
        return capturadas.get(capturadas.size() - 1).getEstado();
    }
}
