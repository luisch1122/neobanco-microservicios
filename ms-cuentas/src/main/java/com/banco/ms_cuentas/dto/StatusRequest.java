package com.banco.ms_cuentas.dto;

import com.banco.ms_cuentas.model.Account.EstadoCuenta;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class StatusRequest {

    @NotNull 
    private EstadoCuenta estadoCuenta;

}
