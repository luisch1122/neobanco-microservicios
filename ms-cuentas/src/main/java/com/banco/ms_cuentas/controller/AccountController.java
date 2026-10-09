package com.banco.ms_cuentas.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banco.ms_cuentas.dto.AccountRequest;
import com.banco.ms_cuentas.model.Account;
import com.banco.ms_cuentas.repository.AccountRepository;
import com.banco.ms_cuentas.service.AccountService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {

        this.accountService = accountService;

    }

    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountRequest accountRequest, 
        Authentication authentication, BindingResult result){

        if (result.hasFieldErrors()) {
            return validation(result);
        }

        Long userId = (Long) authentication.getDetails();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(accountRequest, userId));

    }

    @GetMapping 
    public ResponseEntity<?> listAccount(){
        return null;
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<?> detailAccount(){
        return null;
    }

    @PatchMapping("/{numeroCuenta}/estado")
    public ResponseEntity<?> statusAccount(){
        return null;
    }

    @PostMapping("/{numeroCuenta}/debito")
    public ResponseEntity<?> debit(){
        return null;
    }

    @PostMapping("/{numeroCuenta}/credito")
    public ResponseEntity<?> credit(){
        return null;
    }

    private ResponseEntity<?> validation(BindingResult result) {

        Map<String, String> errors = new HashMap<>();

        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), "El campo " + err.getField() + " " + err.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(errors);
    }

}

