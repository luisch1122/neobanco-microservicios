package com.banco.ms_transacciones.dto;

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
    @DecimalMin("0.01")
    private BigDecimal monto;    

}
