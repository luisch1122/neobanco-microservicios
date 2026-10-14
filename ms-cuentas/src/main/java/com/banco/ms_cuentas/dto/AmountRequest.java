package com.banco.ms_cuentas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class AmountRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "el monto debe ser mayor a 0")
    private BigDecimal monto;

}
