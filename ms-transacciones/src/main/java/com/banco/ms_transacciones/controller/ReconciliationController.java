package com.banco.ms_transacciones.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banco.ms_transacciones.service.ReconciliationService;

@RestController
@RequestMapping("/transactions/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run() {

        reconciliationService.conciliar();

        return ResponseEntity.ok(Map.of("message", "Conciliación ejecutada"));
    }

}
