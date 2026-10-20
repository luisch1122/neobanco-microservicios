package com.banco.ms_transacciones.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Resultado de ejecutar un paso del saga (débito, crédito o compensación)
 * contra ms-cuentas, distinguiendo un fallo definitivo (el servidor rechazó
 * la operación explícitamente) de uno ambiguo (no hubo respuesta: timeout,
 * conexión rechazada, etc., y no se pudo confirmar qué pasó realmente).
 */
record ResultadoPaso(Resultado resultado, HttpStatusCode status, String mensaje) {

    enum Resultado {
        EXITO, FALLO, AMBIGUA
    }

    static ResultadoPaso exito() {
        return new ResultadoPaso(Resultado.EXITO, null, null);
    }

    static ResultadoPaso fallo(HttpStatusCode status, String mensaje) {
        return new ResultadoPaso(Resultado.FALLO, status, mensaje);
    }

    static ResultadoPaso ambigua(String mensaje) {
        return new ResultadoPaso(Resultado.AMBIGUA, HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }

}
