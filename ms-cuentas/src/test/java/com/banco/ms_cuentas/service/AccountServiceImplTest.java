package com.banco.ms_cuentas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.dto.AmountRequest;
import com.banco.ms_cuentas.dto.StatusRequest;
import com.banco.ms_cuentas.model.Account;
import com.banco.ms_cuentas.model.Account.EstadoCuenta;
import com.banco.ms_cuentas.model.Account.TipoCuenta;
import com.banco.ms_cuentas.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountServiceImpl;

    private Account cuentaDe(Long userId, EstadoCuenta estado, BigDecimal saldo) {
        Account account = new Account();
        account.setNumeroCuenta("00101234567");
        account.setUserId(userId);
        account.setTipoCuenta(TipoCuenta.AHORRO);
        account.setMoneda("VES");
        account.setEstadoCuenta(estado);
        account.setSaldo(saldo);
        return account;
    }

    // ---------------------------------------------------------------
    // create
    // ---------------------------------------------------------------

    @Test
    void create_conNumeroDeCuentaDisponible_asignaDatosYGuarda() {
        when(accountRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountRequest request = new AccountRequest(TipoCuenta.CORRIENTE, "VES");

        Account resultado = accountServiceImpl.create(request, 10L);

        assertEquals(10L, resultado.getUserId());
        assertEquals(TipoCuenta.CORRIENTE, resultado.getTipoCuenta());
        assertEquals("VES", resultado.getMoneda());
        assertNotNull(resultado.getNumeroCuenta());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void create_conNumeroDeCuentaColisionado_generaUnoNuevo() {
        when(accountRepository.existsByNumeroCuenta(anyString())).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountRequest request = new AccountRequest(TipoCuenta.AHORRO, "VES");

        Account resultado = accountServiceImpl.create(request, 1L);

        assertNotNull(resultado.getNumeroCuenta());
        verify(accountRepository).save(any(Account.class));
    }

    // ---------------------------------------------------------------
    // list
    // ---------------------------------------------------------------

    @Test
    void list_delegaAlRepositorioPorUserId() {
        when(accountRepository.findByUserId(5L)).thenReturn(java.util.List.of(new Account()));

        var resultado = accountServiceImpl.list(5L);

        assertEquals(1, resultado.size());
        verify(accountRepository).findByUserId(5L);
    }

    // ---------------------------------------------------------------
    // detail
    // ---------------------------------------------------------------

    @Test
    void detail_conCuentaInexistente_lanza404() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.detail("123", 1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void detail_conCuentaDeOtroUsuario_lanza403() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(cuentaDe(1L, EstadoCuenta.ACTIVA, BigDecimal.ZERO));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.detail("123", 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void detail_conPropietarioCorrecto_devuelveLaCuenta() {
        Account account = cuentaDe(1L, EstadoCuenta.ACTIVA, BigDecimal.ZERO);
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(account);

        Account resultado = accountServiceImpl.detail("123", 1L);

        assertEquals(account, resultado);
    }

    // ---------------------------------------------------------------
    // status
    // ---------------------------------------------------------------

    @Test
    void status_bloqueaTransicionHaciaBloqueada() {
        Account account = cuentaDe(1L, EstadoCuenta.ACTIVA, BigDecimal.ZERO);
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(account);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.status("123", new StatusRequest(EstadoCuenta.BLOQUEADA), 1L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void status_bloqueaTransicionDesdeBloqueada() {
        Account account = cuentaDe(1L, EstadoCuenta.BLOQUEADA, BigDecimal.ZERO);
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(account);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.status("123", new StatusRequest(EstadoCuenta.ACTIVA), 1L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void status_permiteTransicionEntreActivaEInactiva() {
        Account account = cuentaDe(1L, EstadoCuenta.ACTIVA, BigDecimal.ZERO);
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account resultado = accountServiceImpl.status("123", new StatusRequest(EstadoCuenta.INACTIVA), 1L);

        assertEquals(EstadoCuenta.INACTIVA, resultado.getEstadoCuenta());
        verify(accountRepository).save(account);
    }

    // ---------------------------------------------------------------
    // debit
    // ---------------------------------------------------------------

    @Test
    void debit_conCuentaDeOtroUsuario_lanza403() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(cuentaDe(1L, EstadoCuenta.ACTIVA, new BigDecimal("100.00")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.debit("123", new AmountRequest(new BigDecimal("10.00")), 2L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void debit_conCuentaBloqueada_lanza403() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(cuentaDe(1L, EstadoCuenta.BLOQUEADA, new BigDecimal("100.00")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.debit("123", new AmountRequest(new BigDecimal("10.00")), 1L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void debit_conFondosInsuficientes_lanza409() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(cuentaDe(1L, EstadoCuenta.ACTIVA, new BigDecimal("5.00")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.debit("123", new AmountRequest(new BigDecimal("10.00")), 1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void debit_conMontoValido_restaSaldoYGuarda() {
        Account account = cuentaDe(1L, EstadoCuenta.ACTIVA, new BigDecimal("100.00"));
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account resultado = accountServiceImpl.debit("123", new AmountRequest(new BigDecimal("40.00")), 1L);

        assertEquals(new BigDecimal("60.00"), resultado.getSaldo());
        verify(accountRepository).save(account);
    }

    // ---------------------------------------------------------------
    // credit
    // ---------------------------------------------------------------

    @Test
    void credit_conCuentaInexistente_lanza404() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.credit("123", new AmountRequest(new BigDecimal("10.00"))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void credit_conCuentaBloqueada_lanza403() {
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(cuentaDe(1L, EstadoCuenta.BLOQUEADA, BigDecimal.ZERO));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accountServiceImpl.credit("123", new AmountRequest(new BigDecimal("10.00"))));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void credit_conMontoValido_sumaSaldoYGuarda() {
        Account account = cuentaDe(1L, EstadoCuenta.ACTIVA, new BigDecimal("50.00"));
        when(accountRepository.findByNumeroCuenta("123")).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account resultado = accountServiceImpl.credit("123", new AmountRequest(new BigDecimal("25.00")));

        assertEquals(new BigDecimal("75.00"), resultado.getSaldo());
        verify(accountRepository).save(account);
    }
}
