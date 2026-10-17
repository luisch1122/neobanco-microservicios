 package com.banco.ms_transacciones.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banco.ms_transacciones.dto.TransactionRequest;
import com.banco.ms_transacciones.service.TransactionService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transaccionService;

    public TransactionController(TransactionService transaccionService){
        this.transaccionService = transaccionService;
    }
    

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransactionRequest transactionRequest, 
        BindingResult result, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){

        if (result.hasFieldErrors()) {
            return validation(result);
        }
        
        return  ResponseEntity.status(HttpStatus.CREATED).body(transaccionService.transfer(transactionRequest, authorization));
    }

    @GetMapping ("/{numeroCuenta}")
    public ResponseEntity<?> transactions(@PathVariable String numeroCuenta, 
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){

        return  ResponseEntity.status(HttpStatus.OK).body(transaccionService.list(numeroCuenta, authorization));

    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detailTranstion(@PathVariable Long id, 
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){

        return  ResponseEntity.status(HttpStatus.OK).body(transaccionService.detail(id, authorization));
        
    }

    private ResponseEntity<?> validation(BindingResult result) {

        Map<String, String> errors = new HashMap<>();

        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), "El campo " + err.getField() + " " + err.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(errors);
    }

}
