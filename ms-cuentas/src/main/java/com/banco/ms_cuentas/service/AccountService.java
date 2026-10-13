package com.banco.ms_cuentas.service;

import java.util.List;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.dto.AmountRequest;
import com.banco.ms_cuentas.dto.StatusRequest;
import com.banco.ms_cuentas.model.Account;

public interface AccountService {

    Account create(AccountRequest accountRepository, Long userId);

    List<Account> list(Long userId);

    Account detail(String numeroCuenta, Long userId);

    Account status(String numeroCuenta, StatusRequest statusRequest, Long userId);

    Account debit(String numeroCuenta, AmountRequest amountRequest, Long userId);

    Account credit(String numeroCuenta, AmountRequest amountRequest);

    String createNumberAccount();

}