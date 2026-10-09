package com.banco.ms_cuentas.service;

import java.util.concurrent.ThreadLocalRandom;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.model.Account;
import com.banco.ms_cuentas.repository.AccountRepository;

public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
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
    public  String createNumberAccount() {

        String numBanco = "0010";
        String numAleatorio = Integer.toString(ThreadLocalRandom.current().nextInt(10000000, 99999999));

        return numBanco + numAleatorio;

    }
        


}
