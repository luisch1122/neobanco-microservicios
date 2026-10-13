# 6. Gestión administrativa de cuentas (desbloqueo) diferida — pendiente de diseño

## Estado
Propuesta (trabajo pendiente, documentado para no perderlo)

## Contexto
Al implementar `PATCH /account/{numeroCuenta}/estado` en `ms-cuentas` surgió la necesidad de permitir **desbloquear** una cuenta en estado `BLOQUEADA`. A diferencia de congelar voluntariamente una cuenta propia (`ACTIVA ⇄ INACTIVA`), el desbloqueo de una cuenta bloqueada no debería quedar en manos del propio usuario dueño de la cuenta — en un banco real, esta operación la realiza personal de **back-office/operaciones**, típicamente tras verificar al cliente o resolver el motivo del bloqueo (fraude, orden judicial, mora, etc.).

Actualmente el sistema **no maneja roles de usuario**: el JWT emitido por `ms-auth-users` solo lleva `email` (subject) y `userId` (claim, ver ADR-0005). No existe ningún concepto de "operador", "administrador" o "personal bancario" distinto de un usuario cliente común.

Se evaluaron tres enfoques:

1. **Rol simple (`ADMIN`) como claim adicional del JWT**: rápido de implementar, pero no refleja cómo funciona un banco real (el personal administrativo no es "un usuario con un flag", es un dominio de negocio distinto con su propia gestión, auditoría y posiblemente su propio flujo de autenticación).
2. **Lista hardcodeada de emails con privilegios administrativos**: solución rápida pero es deuda técnica inaceptable para un sistema que simula operación bancaria real; no escala ni es auditable.
3. **Dominio administrativo separado** (posible futuro `ms-administracion` o un módulo/rol diferenciado dentro de la gestión de identidad, con su propio flujo de autorización): refleja la intención real del proyecto — imitar el funcionamiento de un banco, donde la gestión de cuentas de clientes y las operaciones de back-office son procesos y actores distintos.

## Decisión
Se **difiere** el diseño e implementación de la gestión administrativa de cuentas. Mientras tanto:

- `PATCH /account/{numeroCuenta}/estado` solo permite que el **dueño autenticado de la cuenta** la congele o reactive voluntariamente (`ACTIVA ⇄ INACTIVA`).
- Cualquier intento de transicionar una cuenta hacia o desde el estado `BLOQUEADA` a través de este endpoint es **rechazado** (sin excepción alguna, independientemente de quién lo solicite), ya que todavía no existe ningún actor autorizado en el sistema para realizar esa operación.
- Esta limitación es **intencional y temporal**, no un olvido: se documenta aquí para que quede trazable y se retome cuando se diseñe el dominio administrativo.

## Consecuencias

**Positivas:**
- Evita implementar una solución apresurada (rol hardcodeado) que tendría que rehacerse por completo más adelante.
- Mantiene el sistema seguro por defecto: ninguna cuenta bloqueada puede desbloquearse hasta que exista un mecanismo de autorización administrativa real.
- Dejar esto fuera de alcance por ahora permite diseñarlo con la misma rigurosidad que el resto del sistema (posible microservicio propio, su propio modelo de autenticación para personal bancario, auditoría de quién desbloquea qué cuenta y cuándo).

**Negativas / costos aceptados:**
- Durante el desarrollo y las demostraciones del proyecto, una cuenta que llegue a `BLOQUEADA` **no podrá reactivarse** por ningún medio hasta que se implemente esta funcionalidad — puede requerir una intervención manual directa en la base de datos para fines de prueba.
- Introduce una funcionalidad pendiente más al roadmap (además de `ms-transacciones`, Redis/idempotencia y RabbitMQ), que deberá priorizarse en algún momento si se quiere demostrar el ciclo completo de bloqueo/desbloqueo de una cuenta.

## Próximos pasos (cuando se retome)
- Definir si la gestión administrativa será un microservicio independiente (`ms-administracion`) o un módulo dentro de uno existente.
- Definir el modelo de autorización para personal bancario (¿roles en el mismo JWT con un claim `role`? ¿un sistema de autenticación completamente separado para el back-office?).
- Diseñar auditoría: quién desbloqueó una cuenta, cuándo y con qué justificación (relevante también para `ms-notificaciones`/`ms-auditoria`, ya mencionados en el README).
