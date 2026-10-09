package com.banco.ms_cuentas.service;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.model.Account;

public interface AccountService {

    Account create(AccountRequest accountRepository, Long userId);

    String createNumberAccount();

}