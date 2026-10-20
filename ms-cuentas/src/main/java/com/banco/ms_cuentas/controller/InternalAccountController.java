package com.banco.ms_cuentas.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banco.ms_cuentas.dto.AmountRequest;
import com.banco.ms_cuentas.service.AccountService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/internal/account")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {

        this.accountService = accountService;

    }

    @PostMapping("/{numeroCuenta}/debit")
    public ResponseEntity<?> debit(@PathVariable String numeroCuenta,
        @Valid @RequestBody AmountRequest amountRequest, 
        BindingResult result, 
        Authentication authentication, 
        @RequestHeader("Idempotency-Key") String idempotencyKey){

        if (result.hasFieldErrors()) return validation(result);
        Long userId = (Long) authentication.getDetails();

        return ResponseEntity.ok(accountService.debit(numeroCuenta, amountRequest, userId, idempotencyKey));
    }

    @PostMapping("/{numeroCuenta}/credit")
    public ResponseEntity<?> credit(@PathVariable String numeroCuenta,
        @Valid @RequestBody AmountRequest amountRequest, 
        BindingResult result, 
        Authentication authentication,
        @RequestHeader("Idempotency-Key") String idempotencyKey){
        
        if (result.hasFieldErrors()) return validation(result);
        
        return ResponseEntity.ok(accountService.credit(numeroCuenta, amountRequest, idempotencyKey));
    }

    @GetMapping("/movement/{idempotencyKey}")
    public ResponseEntity<?> movement(@PathVariable String idempotencyKey) {

        return accountService.findMovement(idempotencyKey)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<?> validation(BindingResult result) {

        Map<String, String> errors = new HashMap<>();

        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), "El campo " + err.getField() + " " + err.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(errors);
    }

}
