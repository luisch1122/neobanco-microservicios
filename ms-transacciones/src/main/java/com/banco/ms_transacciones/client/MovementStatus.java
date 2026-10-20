package com.banco.ms_transacciones.client;

/**
 * Resultado de verificar en ms-cuentas si un movimiento (débito, crédito o
 * compensación) realmente se aplicó, usado para recuperar operaciones
 * ambiguas (timeout/error de conexión al llamar a debit/credit).
 */
public enum MovementStatus {

    EXISTE,

    NO_EXISTE,

    DESCONOCIDO

}
