package com.banco.ms_cuentas.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.dto.StatusRequest;
import com.banco.ms_cuentas.model.Account;
import com.banco.ms_cuentas.model.Account.EstadoCuenta;
import com.banco.ms_cuentas.repository.AccountRepository;

import jakarta.transaction.Transactional;

public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public Account create(AccountRequest accountRequest, Long userId) {

        Account account = new Account();
        String numeroCuenta = createNumberAccount();

        if(accountRepository.existsByNumeroCuenta(numeroCuenta)){
            account.setNumeroCuenta(createNumberAccount());
        }else{
            account.setNumeroCuenta(numeroCuenta);
        }

        account.setUserId(userId);
        account.setTipoCuenta(accountRequest.getTipoCuenta());
        account.setMoneda(accountRequest.getMoneda());
        
        return accountRepository.save(account);

    }

    @Override
    public List<Account> list(Long userId) {
        
        return accountRepository.findByUserId(userId);

    }

    @Override 
    public Account detail(String numeroCuenta, Long userId){

        Account account = accountRepository.findByNumeroCuenta(numeroCuenta);

        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");

        if (!account.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta cuenta");

        return account;

    }

    @Override 
    @Transactional 
    public Account status(String numeroCuenta, StatusRequest statusRequest, Long userId){

        Account account = accountRepository.findByNumeroCuenta(numeroCuenta);

        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");

        if (!account.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta cuenta");

        if (account.getEstadoCuenta() == EstadoCuenta.BLOQUEADA 
            || statusRequest.getEstadoCuenta().equals(EstadoCuenta.BLOQUEADA)) {

                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta operación requiere gestión administrativa");

            }
        
        account.setEstadoCuenta(statusRequest.getEstadoCuenta());

        return accountRepository.save(account);

    }

    @Override
    public  String createNumberAccount() {

        String numBanco = "0010";
        String numAleatorio = Integer.toString(ThreadLocalRandom.current().nextInt(10000000, 99999999));

        return numBanco + numAleatorio;

    }   


}
