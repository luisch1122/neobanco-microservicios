# 1. Arquitectura de microservicios en lugar de monolito

## Estado
Aceptada

## Contexto
El proyecto simula un core bancario (NeoBanco) con dominios claramente separables: autenticación y usuarios, cuentas y saldos, transferencias/transacciones, y notificaciones. Cada dominio tiene requisitos de escalado, consistencia y disponibilidad distintos (por ejemplo, las transferencias requieren idempotencia y consistencia fuerte; las notificaciones son asíncronas y tolerantes a fallos).

Además, uno de los objetivos del proyecto es servir como portafolio técnico, por lo que se busca demostrar patrones de diseño distribuido reales (comunicación síncrona/asíncrona, bases de datos por servicio, gateway, despliegue independiente).

## Decisión
Se divide el sistema en microservicios independientes, cada uno con su propio repositorio de código fuente dentro del monorepo, su propio `pom.xml`/build, su propia base de datos PostgreSQL, y su propio contenedor Docker desplegado de forma independiente:

- `ms-auth-users`: registro, login, gestión de usuarios.
- `ms-cuentas`: cuentas y saldos (core banking).
- `ms-transacciones`: transferencias e idempotencia (Redis).
- `ms-notificaciones`: notificaciones en tiempo real (WebSockets/RabbitMQ).

Un gateway (Nginx en `vm-gateway`) enruta las peticiones del frontend hacia cada microservicio y sirve los archivos estáticos del frontend Angular.

## Consecuencias

**Positivas:**
- Cada servicio puede evolucionar, desplegarse y escalar de forma independiente.
- Fallos aislados: un bug en `ms-notificaciones` no tumba el login.
- Permite demostrar comunicación síncrona (REST) y asíncrona (RabbitMQ) en un mismo proyecto.

**Negativas / costos aceptados:**
- Mayor complejidad operativa: múltiples bases de datos, múltiples despliegues, necesidad de una librería compartida (`common-security`) para no duplicar lógica de seguridad.
- No hay transacciones ACID entre servicios (p. ej. entre `ms-cuentas` y `ms-transacciones`); se requieren patrones como Outbox e idempotencia para mantener consistencia eventual.
- Mayor esfuerzo de configuración de infraestructura (3 VMs, Docker, Nginx, CI/CD por servicio) comparado con un monolito desplegable en un solo paso.
