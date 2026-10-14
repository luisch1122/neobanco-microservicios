# 2. Autenticación mediante JWT stateless

## Estado
Aceptada

## Contexto
En una arquitectura de microservicios, cada servicio necesita verificar la identidad del usuario sin depender de una sesión centralizada en memoria (lo cual rompería la independencia y escalabilidad horizontal de cada servicio). Se necesita un mecanismo de autenticación que:

- No requiera que todos los servicios consulten una base de datos de sesiones compartida en cada petición.
- Permita que servicios como `ms-cuentas` o `ms-transacciones` verifiquen la identidad del usuario sin necesidad de llamar por red a `ms-auth-users` en cada request.
- Sea compatible con un gateway Nginx simple (reverse proxy), sin lógica de sesión.

## Decisión
Se adopta JWT (JSON Web Token) como mecanismo de autenticación stateless:

- `ms-auth-users` firma los tokens al autenticar (login) usando una clave simétrica HMAC-SHA256 (`SecretKey`), incluyendo el **email** del usuario como `subject` del token.
- Cada microservicio que necesita proteger endpoints valida el token localmente (firma + expiración) usando un filtro (`JwtValidationFilter`), sin llamar a `ms-auth-users` por red.
- La validación del token se extrajo a la librería compartida `common-security` (ver ADR-0004) para no duplicar esa lógica en cada microservicio.
- Solo `ms-auth-users` tiene la capacidad de **emitir** tokens (`JwtAuthenticationFilter`); el resto de servicios solo los **valida**.

## Consecuencias

**Positivas:**
- Verificación de identidad rápida y sin dependencia de red entre microservicios en cada petición.
- Escalabilidad horizontal simple: cualquier instancia de cualquier servicio puede validar el token siempre que comparta el mismo secreto.
- Separación clara de responsabilidades: solo un servicio emite tokens, todos los demás solo los validan.

**Negativas / costos aceptados:**
- Revocar un token antes de su expiración no es trivial (JWT es stateless por diseño); no se implementó una lista de revocación/blacklist todavía.
- Todos los microservicios que validan tokens deben compartir el mismo secreto de firma (`JWT_SECRET`), lo que crea un acoplamiento de configuración entre servicios (mitigado parcialmente centralizando el secreto como variable de entorno/secreto de CI, ver ADR-0003).
- El token solo lleva el `email` como identificador; si en el futuro se necesita un `userId` numérico interno, habrá que añadirlo como claim adicional (ver ADR-0005 para el impacto actual de esta limitación).
