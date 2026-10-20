package com.banco.ms_transacciones.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Utilidades para clasificar errores al llamar a ms-cuentas desde
 * ms-transacciones. Compartidas entre {@code TransactionServiceImpl} (saga en
 * línea) y {@code ReconciliationService} (conciliación distribuida) para
 * evitar duplicar la lógica de interpretación de errores HTTP.
 */
public final class RestClientErrorUtils {

    private RestClientErrorUtils() {
    }

    public static String getErrorMessage(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseEx) {
            return responseEx.getResponseBodyAsString();
        }
        return "No fue posible conectar con el servicio de cuentas";
    }

    public static HttpStatusCode getErrorStatus(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseEx) {
            return responseEx.getStatusCode();
        }
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    /**
     * Un error es "ambiguo" cuando no hay una respuesta HTTP explícita del
     * servidor (timeout, conexión rechazada, DNS, etc.): no sabemos si la
     * operación remota llegó a aplicarse o no. Si hubo respuesta HTTP
     * (4xx/5xx), el servidor sí procesó la petición y el resultado es
     * definitivo, no ambiguo.
     */
    public static boolean esAmbigua(RestClientException ex) {
        return !(ex instanceof RestClientResponseException);
    }

}
