package com.banco.ms_transacciones.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "transaccion")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @NotBlank
    @Column(nullable = false)
    private String cuentaOrigen;

    @NotBlank
    @Column(nullable = false)
    private String cuentaDestino;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTransaccion estado = EstadoTransaccion.PENDIENTE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransaccion tipo = TipoTransaccion.TRANSFERENCIA;

    private String motivoFallo;

    @Enumerated(EnumType.STRING)
    private PasoSaga pasoPendiente;

    @Column(nullable = false)
    private Integer intentosConciliacion = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    private LocalDateTime actualizadoEn;

    public enum EstadoTransaccion {
        PENDIENTE, COMPLETADA, FALLIDA, REVERTIDA, FALLO_CRITICO_SIN_COMPENSAR, PENDIENTE_VERIFICACION
    }

    public enum TipoTransaccion {
        TRANSFERENCIA
    }

    public enum PasoSaga {
        DEBITO_ORIGEN, CREDITO_DESTINO, COMPENSACION_ORIGEN
    }

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
        if (actualizadoEn == null) actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }    

}
