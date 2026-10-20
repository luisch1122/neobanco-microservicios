package com.banco.ms_transacciones;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.banco.ms_transacciones.client.AccountClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Pruebas de integracion del flujo de transacciones ejecutadas contra el
 * contexto real de Spring (MockMvc), el filtro JWT real de
 * common-security y una base de datos H2 en memoria (perfil "test").
 *
 * AccountClient (la llamada HTTP saliente hacia ms-cuentas) se reemplaza
 * por un mock via @MockitoBean, ya que estas pruebas no deben depender
 * de que ms-cuentas este realmente desplegado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountClient accountClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String tokenPara(String email, Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return "Bearer " + Jwts.builder()
                .subject(email)
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    private String transferRequestBody(String idempotencyKey, String origen, String destino, String monto) {
        return """
                {
                  "idempotencyKey": "%s",
                  "cuentaOrigen": "%s",
                  "cuentaDestino": "%s",
                  "monto": %s
                }
                """.formatted(idempotencyKey, origen, destino, monto);
    }

    // ---------------------------------------------------------------
    // Seguridad general
    // ---------------------------------------------------------------

    @Test
    void sinToken_cualquierRutaDeTransaccionesNoEstaAutorizada() throws Exception {
        mockMvc.perform(get("/transactions/account/001"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(statusCode).isIn(401, 403);
                });
    }

    // ---------------------------------------------------------------
    // POST /transactions/transfer
    // ---------------------------------------------------------------

    @Test
    void transfer_conDatosValidosYServiciosOk_devuelve201ConEstadoCompletada() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), anyString(), eq("key-int-1:DEBITO"));
        doNothing().when(accountClient).credit(eq("002"), any(BigDecimal.class), anyString(), eq("key-int-1:CREDITO"));

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequestBody("key-int-1", "001", "002", "50.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.cuentaOrigen").value("001"))
                .andExpect(jsonPath("$.cuentaDestino").value("002"));
    }

    @Test
    void transfer_conMismaCuentaOrigenYDestino_devuelve409() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequestBody("key-int-2", "001", "001", "50.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void transfer_conDatosInvalidos_devuelve400() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuentaOrigen\": \"001\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_conMismaIdempotencyKeyDosVeces_noVuelveADebitarNiCreditar() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), anyString(), eq("key-int-3:DEBITO"));
        doNothing().when(accountClient).credit(eq("002"), any(BigDecimal.class), anyString(), eq("key-int-3:CREDITO"));

        String body = transferRequestBody("key-int-3", "001", "002", "15.00");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        org.mockito.Mockito.verify(accountClient, org.mockito.Mockito.times(1))
                .debit(eq("001"), any(BigDecimal.class), anyString(), eq("key-int-3:DEBITO"));
    }

    @Test
    void transfer_conDebitoRechazadoPorMsCuentas_devuelveElMismoCodigoYMarcaFallida() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        doThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Fondos insuficientes", null, null, null))
                .when(accountClient).debit(eq("001"), any(BigDecimal.class), anyString(), eq("key-int-4:DEBITO"));

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequestBody("key-int-4", "001", "002", "15.00")))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // GET /transactions/account/{numeroCuenta}
    // ---------------------------------------------------------------

    @Test
    void listarPorCuenta_conPropietarioValido_devuelveLasTransaccionesDeEsaCuenta() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), anyString(), eq("key-int-5:DEBITO"));
        doNothing().when(accountClient).credit(eq("002"), any(BigDecimal.class), anyString(), eq("key-int-5:CREDITO"));

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequestBody("key-int-5", "001", "002", "20.00")))
                .andExpect(status().isCreated());

        doNothing().when(accountClient).verify(eq("001"), anyString());

        mockMvc.perform(get("/transactions/account/001").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarPorCuenta_sinPermisoEnMsCuentas_devuelve403() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "No autorizado", null, null, null))
                .when(accountClient).verify(eq("999"), anyString());

        mockMvc.perform(get("/transactions/account/999").header("Authorization", authorization))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // GET /transactions/{id}
    // ---------------------------------------------------------------

    @Test
    void detalle_conTransaccionInexistente_devuelve404() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);

        mockMvc.perform(get("/transactions/999999").header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalle_conPropietarioDeCuentaDestino_devuelve200() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        doNothing().when(accountClient).debit(eq("001"), any(BigDecimal.class), anyString(), eq("key-int-6:DEBITO"));
        doNothing().when(accountClient).credit(eq("002"), any(BigDecimal.class), anyString(), eq("key-int-6:CREDITO"));

        String response = mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequestBody("key-int-6", "001", "002", "30.00")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        doNothing().when(accountClient).verify(eq("002"), anyString());

        mockMvc.perform(get("/transactions/" + id).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }
}
