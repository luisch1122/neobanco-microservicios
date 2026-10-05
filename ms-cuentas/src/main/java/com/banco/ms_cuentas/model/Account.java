package com.banco.ms_cuentas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuentas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroCuenta;

    @NotBlank 
    @Column(nullable = false)
    private String usuarioEmail;

    @NotNull 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCuenta tipoCuenta;

    @NotBlank 
    @Column(nullable = false)
    private String moneda = "VES";

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private EstadoCuenta estadoCuenta = EstadoCuenta.ACTIVA;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;    

    private LocalDateTime actualizadoEn;

    public enum TipoCuenta {
        AHORRO,
        CORRIENTE
    }

    public enum EstadoCuenta {
        ACTIVA,
        INACTIVA,
        BLOQUEADA
    }

    @PrePersist
    public void prePersist() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
        if (this.actualizadoEn == null) {
            this.actualizadoEn = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
