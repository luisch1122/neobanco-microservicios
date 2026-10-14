package com.banco.ms_transacciones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/transactions")
public class TransactionController {

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(){
        return null;
    }

    @GetMapping 
    public ResponseEntity<?> transactions(){
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detailTranstion(){
        return null;
    }

}
