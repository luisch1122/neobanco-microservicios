package com.banco.ms_transacciones.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.banco.ms_transacciones.dto.AmountRequest;

@Component 
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(@Value("${ms-cuentas.base-url}") String baseUrl){
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void debit(String numeroCuenta, BigDecimal amont, String auth) {

        restClient.post()
            .uri("/internal/account/{numeroCuenta}/debit", numeroCuenta)
            .header("Authorization", auth)
            .body(new AmountRequest(amont))
            .retrieve()
            .toBodilessEntity();
    }

    public void credit(String numeroCuenta, BigDecimal amont, String auth){
        restClient.post()
            .uri("/internal/account/{numeroCuenta}/credit", numeroCuenta)
            .header("Authorization", auth)
            .body(new AmountRequest(amont))
            .retrieve()
            .toBodilessEntity();
    }

    public void verify(String numeroCuenta, String auth){
        restClient.get()
            .uri("/account/{numeroCuenta}", numeroCuenta)
            .header("Authorization", auth)
            .retrieve()
            .toBodilessEntity();
    }

}
