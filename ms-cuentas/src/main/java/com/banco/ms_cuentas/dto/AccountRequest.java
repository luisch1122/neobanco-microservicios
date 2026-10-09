package com.banco.ms_cuentas.dto;

import com.banco.ms_cuentas.model.Account.TipoCuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class AccountRequest {

    @NotNull 
    private TipoCuenta tipoCuenta;

    @NotBlank 
    private String moneda = "VES";

}
