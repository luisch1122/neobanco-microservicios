# 3. Externalización del secreto de firma JWT

## Estado
Aceptada

## Contexto
La implementación original de `TokenJwtConfig` generaba la clave de firma JWT (`SecretKey`) de forma **aleatoria en cada arranque de la JVM** (`Jwts.SIG.HS256.key().build()`). Esto no era solo un valor hardcodeado inseguro, sino un defecto de diseño activo:

- Cada reinicio del servicio invalidaba todos los tokens emitidos previamente (los usuarios debían volver a iniciar sesión).
- Si se escalaba `ms-auth-users` horizontalmente (varias instancias), cada instancia generaría una clave distinta, por lo que un token emitido por una instancia no sería válido en otra.
- Esto además bloqueaba la extracción de la validación de JWT a una librería compartida (`common-security`, ver ADR-0004): no tiene sentido compartir lógica de validación si cada instancia usa una clave diferente e impredecible.

## Decisión
Se externaliza el secreto de firma como configuración:

- `application.properties` define `jwt.secret=${JWT_SECRET:<valor-default-solo-para-desarrollo-local>}`.
- Un `@Bean SecretKey jwtSecretKey()` en `SecurityConfig` construye la clave real a partir del valor Base64 de `jwt.secret` mediante `Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret))`.
- El valor real de `JWT_SECRET` se gestiona como secreto de GitHub Actions y se inyecta al contenedor en tiempo de despliegue (`-e JWT_SECRET=${{ secrets.JWT_SECRET }}` en el `docker run` del workflow de CI/CD), nunca se commitea en el repositorio.
- El `SecretKey` resultante se inyecta por constructor tanto en `JwtAuthenticationFilter` (emisión) como en `JwtValidationFilter` (validación).

## Consecuencias

**Positivas:**
- Los tokens sobreviven a reinicios del servicio (misma clave siempre que `JWT_SECRET` no cambie).
- Todas las instancias de un mismo servicio (y de otros servicios que validen JWT) comparten la misma clave, permitiendo escalado horizontal real.
- El secreto nunca queda expuesto en el código fuente ni en el historial de Git.

**Negativas / costos aceptados:**
- Si se necesita rotar el secreto, todos los tokens emitidos previamente se invalidan de golpe (no hay rotación gradual con múltiples claves activas simultáneamente).
- Requiere gestión manual del secreto en GitHub Actions y coordinación entre el entorno de desarrollo local (valor por defecto) y producción (valor real inyectado en despliegue).
