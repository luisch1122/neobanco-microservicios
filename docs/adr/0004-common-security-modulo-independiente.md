# 4. `common-security` como módulo Maven independiente (no monorepo reactor)

## Estado
Aceptada

## Contexto
La lógica de validación de JWT (`JwtValidationFilter`, constantes de `TokenJwtConfig`, construcción del `SecretKey` desde una propiedad) debía dejar de vivir únicamente en `ms-auth-users`, ya que `ms-cuentas` y los futuros microservicios (`ms-transacciones`, `ms-notificaciones`) también necesitan validar JWT sin duplicar esa lógica ni depender en tiempo de ejecución de `ms-auth-users`.

Se evaluaron dos estrategias para compartir este código:

1. **Opción 1 — Módulos Maven en un reactor/monorepo**: un `pom.xml` padre agregador con módulos (`<modules>`), donde `common-security` se compila junto con los demás en cada build. Cambios en la librería se propagan de inmediato a los consumidores sin publicar nada.
2. **Opción 2 — Módulo independiente publicado a un registro Maven** (GitHub Packages), versionado semánticamente y consumido como una dependencia normal (`<dependency>`) por cada microservicio.

## Decisión
Se elige la **Opción 2**: `common-security` es un módulo Maven completamente independiente, con su propio ciclo de vida de versión (`0.1.0-SNAPSHOT` por ahora), publicado mediante `mvn deploy` al registro de GitHub Packages del repositorio (`https://maven.pkg.github.com/luisch1122/neobanco-microservicios`). Cada microservicio consumidor (`ms-auth-users`, `ms-cuentas`) lo declara como una dependencia Maven normal con su propio `<repositories>` apuntando a ese registro.

## Consecuencias

**Positivas:**
- Refleja una práctica real de la industria: las librerías compartidas entre microservicios se versionan y publican de forma independiente, no se recompilan junto con cada consumidor.
- Los `Dockerfile` de cada microservicio permanecen simples (contexto de build de una sola carpeta), sin necesidad de construir todo el reactor para generar la imagen de un solo servicio.
- Permite, a futuro, que distintos microservicios consuman distintas versiones de `common-security` durante una migración gradual.

**Negativas / costos aceptados:**
- Cada cambio en `common-security` requiere un paso explícito de publicación (`mvn deploy`) y, usualmente, un bump de versión — no se propaga automáticamente a los consumidores como en un monorepo reactor.
- Requiere configurar credenciales (GitHub PAT con `write:packages`/`read:packages`) tanto para publicar como para consumir, tanto en entornos locales (`settings.xml`) como en CI.
- Mientras `common-security` no esté publicada en GitHub Packages (solo instalada en el `.m2` local de cada desarrollador), el build de CI de los microservicios consumidores no podrá resolver la dependencia — pendiente de automatizar un workflow de publicación.
