# 5. Identificación del usuario entre microservicios mediante email (sin FK cruzada)

## Estado
Aceptada

## Contexto
`ms-cuentas` necesita asociar cada cuenta bancaria a un usuario. En una arquitectura de microservicios con **una base de datos por servicio** (ver ADR-0001), no es posible (ni deseable) tener una clave foránea (`FOREIGN KEY`) de la tabla `cuentas` (en la base de datos de `ms-cuentas`) hacia la tabla `usuarios` (en la base de datos de `ms-auth-users`): son motores/instancias de PostgreSQL distintos.

Además, el JWT emitido por `ms-auth-users` (ver ADR-0002) solo incluye el **email** del usuario como `subject` del token — no incluye el `id` numérico interno (`Long id` autogenerado) de la tabla `usuarios`.

## Decisión
La entidad `Account` (`ms-cuentas`) almacena el campo `usuarioEmail` (String) como identificador del dueño de la cuenta, en lugar de un `usuarioId` numérico con relación JPA (`@ManyToOne`/`@JoinColumn`). Este valor se obtiene directamente del `subject` del JWT validado por `common-security` (ver ADR-0002 y ADR-0004), sin necesidad de llamar por red a `ms-auth-users` para resolverlo.

## Consecuencias

**Positivas:**
- No se requieren llamadas síncronas entre `ms-cuentas` y `ms-auth-users` para resolver la identidad del dueño de una cuenta; el dato ya viene validado en el token.
- Evita el antipatrón de intentar una FK real entre bases de datos de servicios distintos.
- Mantiene a `ms-cuentas` completamente autónomo: puede operar aunque `ms-auth-users` esté caído, siempre que el token ya haya sido emitido y no haya expirado.

**Negativas / costos aceptados:**
- El email se convierte en el identificador "de facto" del usuario a través de los microservicios. Si un usuario cambia su email (`PUT /auth/me`), las cuentas asociadas en `ms-cuentas` quedarían huérfanas (referenciando el email antiguo) — **no resuelto todavía**; una futura iteración debería propagar el cambio de email (por ejemplo, vía evento en RabbitMQ) o migrar a usar un `userId` estable como claim adicional del JWT.
- Es más frágil que usar un identificador inmutable (como un UUID o el `id` numérico): el email, aunque poco frecuente, es mutable por diseño en `ms-auth-users`.
