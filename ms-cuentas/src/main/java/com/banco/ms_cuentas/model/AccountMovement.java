package com.banco.ms_cuentas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banco.ms_cuentas.model.Account.TipoCuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuentas_movientos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @NotBlank
    @Column(nullable = false)
    private String cuenta;

    @NotNull 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public enum Tipo {
        DEBITO,
        CREDITO,
        COMPENSACION
    }

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
