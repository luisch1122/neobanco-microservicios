package com.banco.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de integracion del flujo completo de registro, login y
 * validacion de JWT expuesto por ms-auth-users, ejecutadas contra el
 * contexto real de Spring (MockMvc) y una base de datos H2 en memoria
 * (perfil "test"). Cada prueba corre dentro de una transaccion que se
 * revierte al finalizar, por lo que los datos no persisten entre tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    private static final String PASSWORD = "Sup3rSecreta!";

    @Autowired
    private MockMvc mockMvc;

    private String randomEmail() {
        return "usuario-" + UUID.randomUUID() + "@example.com";
    }

    private String registerRequestBody(String nombre, String email, String password) {
        return """
                {
                  "nombre": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(nombre, email, password);
    }

    private String loginRequestBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    // ---------------------------------------------------------------
    // /auth/register
    // ---------------------------------------------------------------

    @Test
    void registrar_conDatosValidos_devuelve201YNoExponeLaContrasenaEnTextoPlano() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Ana Perez", email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").value(not(PASSWORD)));
    }

    @Test
    void registrar_conEmailYaExistente_devuelve400ConErrorDeValidacion() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Ana Perez", email, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Otro Nombre", email, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void registrar_conCamposVacios_devuelve400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("", "", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    // ---------------------------------------------------------------
    // /auth/login
    // ---------------------------------------------------------------

    @Test
    void login_conCredencialesValidas_devuelveTokenJwtYCabeceraAuthorization() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Ana Perez", email, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(header().string("Authorization", containsString("Bearer ")));
    }

    @Test
    void login_conPasswordIncorrecta_devuelve401() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Ana Perez", email, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(email, "contrasena-incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conEmailInexistente_devuelve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(randomEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // JwtValidationFilter (rutas protegidas)
    // ---------------------------------------------------------------

    @Test
    void rutaProtegida_sinToken_noEsAutorizada() throws Exception {
        mockMvc.perform(get("/auth/perfil"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void rutaProtegida_conTokenInvalido_devuelve401ConMensaje() throws Exception {
        mockMvc.perform(get("/auth/perfil")
                .header("Authorization", "Bearer token-invalido-o-manipulado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("El token JWT no es valido"));
    }

    @Test
    void rutaProtegida_conTokenValido_pasaLaAutenticacionYLlegaAlEndpointDeNegocio() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Ana Perez", email, PASSWORD)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization");

        // Un token valido debe llegar hasta el endpoint de negocio protegido
        // (/auth/me) y devolver 200 con el perfil del usuario autenticado.
        mockMvc.perform(get("/auth/me")
                .header("Authorization", loginResponse))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    // ---------------------------------------------------------------
    // /auth/me (GET, PUT) y /auth/me/password (PUT)
    // ---------------------------------------------------------------

    private String updateProfileRequestBody(String nombre, String email) {
        return """
                {
                  "nombre": "%s",
                  "email": "%s"
                }
                """.formatted(nombre, email);
    }

    private String updatePasswordRequestBody(String currentPassword, String newPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(currentPassword, newPassword);
    }

    /** Registra y loguea un usuario nuevo, devolviendo su email y el header Authorization (con el prefijo Bearer). */
    private String[] registrarYLoguear(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("Ana Perez", email, PASSWORD)))
                .andExpect(status().isCreated());

        String authorizationHeader = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Authorization");

        return new String[] { email, authorizationHeader };
    }

    @Test
    void getMe_conTokenValido_devuelvePerfilSinExponerLaContrasena() throws Exception {
        String email = randomEmail();
        String authorization = registrarYLoguear(email)[1];

        mockMvc.perform(get("/auth/me")
                .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void putMe_conDatosValidos_actualizaNombreYEmail() throws Exception {
        String email = randomEmail();
        String authorization = registrarYLoguear(email)[1];
        String nuevoEmail = randomEmail();

        mockMvc.perform(put("/auth/me")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateProfileRequestBody("Ana Perez Actualizada", nuevoEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Perez Actualizada"))
                .andExpect(jsonPath("$.email").value(nuevoEmail))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void putMe_conEmailYaUsadoPorOtroUsuario_devuelve400() throws Exception {
        String emailA = randomEmail();
        String emailB = randomEmail();

        String authorizationA = registrarYLoguear(emailA)[1];
        registrarYLoguear(emailB);

        mockMvc.perform(put("/auth/me")
                .header("Authorization", authorizationA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateProfileRequestBody("Ana Perez", emailB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void putMePassword_conContrasenaActualCorrecta_actualizaLaContrasena() throws Exception {
        String email = randomEmail();
        String authorization = registrarYLoguear(email)[1];
        String nuevaPassword = "NuevaSup3rSecreta!";

        mockMvc.perform(put("/auth/me/password")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePasswordRequestBody(PASSWORD, nuevaPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());

        // El login con la contrasena anterior ya no debe funcionar...
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        // ...y el login con la nueva contrasena si debe funcionar.
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody(email, nuevaPassword)))
                .andExpect(status().isOk());
    }

    @Test
    void putMePassword_conContrasenaActualIncorrecta_devuelve400() throws Exception {
        String email = randomEmail();
        String authorization = registrarYLoguear(email)[1];

        mockMvc.perform(put("/auth/me/password")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePasswordRequestBody("contrasena-incorrecta", "NuevaSup3rSecreta!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña actual no es correcta"));
    }

    @Test
    void putMePassword_conNuevaContrasenaIgualALaActual_devuelve400() throws Exception {
        String email = randomEmail();
        String authorization = registrarYLoguear(email)[1];

        mockMvc.perform(put("/auth/me/password")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePasswordRequestBody(PASSWORD, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La nueva contraseña debe ser diferente a la actual"));
    }
}

