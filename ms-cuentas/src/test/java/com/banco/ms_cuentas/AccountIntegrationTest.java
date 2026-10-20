package com.banco.ms_cuentas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.banco.ms_cuentas.model.Account;
import com.banco.ms_cuentas.model.AccountMovement;
import com.banco.ms_cuentas.model.AccountMovement.Tipo;
import com.banco.ms_cuentas.repository.AccountMovementRepository;
import com.banco.ms_cuentas.repository.AccountRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Pruebas de integracion del flujo de cuentas (crear, listar, detalle,
 * cambio de estado, debito/credito interno) ejecutadas contra el
 * contexto real de Spring (MockMvc), el filtro JWT real de
 * common-security, y una base de datos H2 en memoria (perfil "test").
 *
 * Como ms-cuentas no emite tokens (eso es responsabilidad de
 * ms-auth-users), los tokens de prueba se firman aqui mismo con el
 * mismo jwt.secret configurado para el perfil de test, imitando el
 * formato real: subject = email, claim "userId" = Long.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMovementRepository accountMovementRepository;

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

    private String tokenSinUserId(String email) {
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

        return "Bearer " + Jwts.builder()
                .subject(email)
                .signWith(secretKey)
                .compact();
    }

    private String createAccountRequestBody(String tipoCuenta, String moneda) {
        return """
                {
                  "tipoCuenta": "%s",
                  "moneda": "%s"
                }
                """.formatted(tipoCuenta, moneda);
    }

    private String statusRequestBody(String estadoCuenta) {
        return """
                {
                  "estadoCuenta": "%s"
                }
                """.formatted(estadoCuenta);
    }

    private String amountRequestBody(String monto) {
        return """
                {
                  "monto": %s
                }
                """.formatted(monto);
    }

    private void fijarSaldo(String numeroCuenta, String saldo) {
        Account account = accountRepository.findByNumeroCuenta(numeroCuenta);
        account.setSaldo(new BigDecimal(saldo));
        accountRepository.save(account);
    }

    private void registrarDebitoAsociado(String numeroCuenta, String idempotencyKey, String monto) {
        AccountMovement movement = new AccountMovement();
        movement.setIdempotencyKey(idempotencyKey);
        movement.setCuenta(numeroCuenta);
        movement.setTipo(Tipo.DEBITO);
        movement.setMonto(new BigDecimal(monto));
        accountMovementRepository.save(movement);
    }

    /** Crea una cuenta para el usuario dado y devuelve su numeroCuenta. */
    private String crearCuentaYObtenerNumero(String authorization) throws Exception {
        String response = mockMvc.perform(post("/account")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccountRequestBody("AHORRO", "VES")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.numeroCuenta");
    }

    // ---------------------------------------------------------------
    // Seguridad general
    // ---------------------------------------------------------------

    @Test
    void sinToken_cualquierRutaDeCuentasNoEstaAutorizada() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(statusCode).isIn(401, 403);
                });
    }

    @Test
    void tokenSinUserId_noPuedeCrearCuenta() throws Exception {
        mockMvc.perform(post("/account")
                .header("Authorization", tokenSinUserId("usuario1@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccountRequestBody("AHORRO", "VES")))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // POST /account
    // ---------------------------------------------------------------

    @Test
    void createAccount_conDatosValidos_devuelve201ConCuentaActivaYSaldoCero() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);

        mockMvc.perform(post("/account")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccountRequestBody("AHORRO", "VES")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta").isNotEmpty())
                .andExpect(jsonPath("$.estadoCuenta").value("ACTIVA"))
                .andExpect(jsonPath("$.saldo").value(0));
    }

    @Test
    void createAccount_conTipoCuentaInvalido_devuelve400() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);

        mockMvc.perform(post("/account")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"moneda\": \"VES\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tipoCuenta").exists());
    }

    // ---------------------------------------------------------------
    // GET /account
    // ---------------------------------------------------------------

    @Test
    void listAccount_devuelveSoloLasCuentasDelUsuarioAutenticado() throws Exception {
        String authUsuario1 = tokenPara("usuario1@example.com", 1L);
        String authUsuario2 = tokenPara("usuario2@example.com", 2L);

        crearCuentaYObtenerNumero(authUsuario1);
        crearCuentaYObtenerNumero(authUsuario2);

        mockMvc.perform(get("/account").header("Authorization", authUsuario1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------------------------------------------------------------
    // GET /account/{numeroCuenta}
    // ---------------------------------------------------------------

    @Test
    void detailAccount_conPropietarioCorrecto_devuelve200() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);

        mockMvc.perform(get("/account/" + numeroCuenta).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value(numeroCuenta));
    }

    @Test
    void detailAccount_conOtroUsuario_devuelve403() throws Exception {
        String authPropietario = tokenPara("usuario1@example.com", 1L);
        String authOtro = tokenPara("usuario2@example.com", 2L);
        String numeroCuenta = crearCuentaYObtenerNumero(authPropietario);

        mockMvc.perform(get("/account/" + numeroCuenta).header("Authorization", authOtro))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailAccount_conNumeroInexistente_devuelve404() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);

        mockMvc.perform(get("/account/00109999999").header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // PATCH /account/{numeroCuenta}/status
    // ---------------------------------------------------------------

    @Test
    void statusAccount_cambiaDeActivaAInactiva_devuelve200() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);

        mockMvc.perform(patch("/account/" + numeroCuenta + "/status")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusRequestBody("INACTIVA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCuenta").value("INACTIVA"));
    }

    @Test
    void statusAccount_intentaBloquearLaCuenta_devuelve403() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);

        mockMvc.perform(patch("/account/" + numeroCuenta + "/status")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusRequestBody("BLOQUEADA")))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // POST /internal/account/{numeroCuenta}/debit y /credit
    // ---------------------------------------------------------------

    @Test
    void debit_conFondosSuficientes_descuentaSaldo() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);
        fijarSaldo(numeroCuenta, "100.00");

        mockMvc.perform(post("/internal/account/" + numeroCuenta + "/debit")
                .header("Authorization", authorization)
            .header("Idempotency-Key", "integration-debit:DEBITO")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amountRequestBody("40.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(60.00));
    }

    @Test
    void debit_conFondosInsuficientes_devuelve409() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);

        mockMvc.perform(post("/internal/account/" + numeroCuenta + "/debit")
                .header("Authorization", authorization)
            .header("Idempotency-Key", "integration-insufficient:DEBITO")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amountRequestBody("10.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void credit_sumaSaldo() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);
        registrarDebitoAsociado(numeroCuenta, "integration-credit:DEBITO", "25.00");

        mockMvc.perform(post("/internal/account/" + numeroCuenta + "/credit")
                .header("Authorization", authorization)
            .header("Idempotency-Key", "integration-credit:CREDITO")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amountRequestBody("25.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(25.00));
    }

    @Test
    void credit_sinDebitoAsociado_devuelve403() throws Exception {
        String authorization = tokenPara("usuario1@example.com", 1L);
        String numeroCuenta = crearCuentaYObtenerNumero(authorization);

        mockMvc.perform(post("/internal/account/" + numeroCuenta + "/credit")
                .header("Authorization", authorization)
                .header("Idempotency-Key", "integration-orphan:CREDITO")
                .contentType(MediaType.APPLICATION_JSON)
                .content(amountRequestBody("25.00")))
                .andExpect(status().isForbidden());
    }
}
