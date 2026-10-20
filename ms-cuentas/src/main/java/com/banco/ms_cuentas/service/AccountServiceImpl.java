package com.banco.ms_cuentas.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.dto.AmountRequest;
import com.banco.ms_cuentas.dto.StatusRequest;
import com.banco.ms_cuentas.model.Account;
import com.banco.ms_cuentas.model.AccountMovement;
import com.banco.ms_cuentas.model.Account.EstadoCuenta;
import com.banco.ms_cuentas.model.AccountMovement.Tipo;
import com.banco.ms_cuentas.repository.AccountMovementRepository;
import com.banco.ms_cuentas.repository.AccountRepository;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMovementRepository accountMovementRepository;

    public AccountServiceImpl(AccountRepository accountRepository,
        AccountMovementRepository accountMovementRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountMovementRepository = accountMovementRepository;
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
    @Transactional 
    public Account debit(String numeroCuenta, AmountRequest amountRequest, Long userId, String idempotencyKey) {
        
        Account account = accountRepository.findByNumeroCuentaForUpdate(numeroCuenta);

        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");

        if (!account.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta cuenta");

        Optional<AccountMovement> existing = accountMovementRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()){

            AccountMovement previous = existing.get();

            boolean sameData =
                    Objects.equals(previous.getCuenta(), numeroCuenta)
                    && previous.getTipo() == Tipo.DEBITO
                    && previous.getMonto() != null
                    && previous.getMonto().compareTo(amountRequest.getMonto()) == 0;

            if (!sameData) {

                throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada con otros datos");
            
            }

            return account;

        }

        if (account.getEstadoCuenta().equals(EstadoCuenta.BLOQUEADA) 
            || account.getEstadoCuenta().equals(EstadoCuenta.INACTIVA)){

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta Bloqueada / Inactiva");

        }

        if (account.getSaldo().compareTo(amountRequest.getMonto()) < 0){

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fondos insuficientes");

        }

        AccountMovement accountMovement = new AccountMovement();

        accountMovement.setIdempotencyKey(idempotencyKey);
        accountMovement.setCuenta(numeroCuenta);
        accountMovement.setTipo(Tipo.DEBITO);
        accountMovement.setMonto(amountRequest.getMonto());

        accountMovementRepository.save(accountMovement);
        
        account.setSaldo(account.getSaldo().subtract(amountRequest.getMonto()));

        return accountRepository.save(account);
    }  

    @Override
    @Transactional 
    public Account credit(String numeroCuenta, AmountRequest amountRequest, String idempotencyKey) {

        Account account = accountRepository.findByNumeroCuentaForUpdate(numeroCuenta);

        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");

        int separator = idempotencyKey.lastIndexOf(':');

        if (separator < 0 || separator == idempotencyKey.length() - 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Clave de idempotencia inválida");
        }

        Tipo tipo;

        try {
            tipo = Tipo.valueOf(idempotencyKey.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de movimiento inválido");
        }

        if (tipo != Tipo.CREDITO && tipo != Tipo.COMPENSACION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Este endpoint solo permite créditos");
        }

        String prefijo = idempotencyKey.substring(0, separator);

        boolean debitoExiste = accountMovementRepository
            .findByIdempotencyKey(prefijo + ":DEBITO")
            .isPresent();

        if (!debitoExiste) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No existe un débito asociado a esta operación");
        }

        Optional<AccountMovement> existing =
            accountMovementRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            AccountMovement previous = existing.get();

            boolean sameData =
                    Objects.equals(previous.getCuenta(), numeroCuenta)
                    && previous.getTipo() == tipo
                    && previous.getMonto() != null
                    && previous.getMonto().compareTo(amountRequest.getMonto()) == 0;

            if (!sameData) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave de idempotencia ya fue usada con otros datos");
            }

            return account;
        }


        if (account.getEstadoCuenta().equals(EstadoCuenta.BLOQUEADA) 
            || account.getEstadoCuenta().equals(EstadoCuenta.INACTIVA)){

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta Bloqueada / Inactiva");

        }

        AccountMovement accountMovement = new AccountMovement();

        accountMovement.setIdempotencyKey(idempotencyKey);
        accountMovement.setCuenta(numeroCuenta);
        accountMovement.setTipo(tipo);
        accountMovement.setMonto(amountRequest.getMonto());

        accountMovementRepository.save(accountMovement);

        account.setSaldo(account.getSaldo().add(amountRequest.getMonto()));

        return accountRepository.save(account);

    }

    @Override
    public Optional<AccountMovement> findMovement(String idempotencyKey) {

        return accountMovementRepository.findByIdempotencyKey(idempotencyKey);

    }

    @Override
    public  String createNumberAccount() {

        String numBanco = "0010";
        String numAleatorio = Integer.toString(ThreadLocalRandom.current().nextInt(10000000, 99999999));

        return numBanco + numAleatorio;

    } 


}
