# Architecture Decision Records (ADR)

Este directorio documenta las decisiones arquitectónicas importantes tomadas durante el desarrollo de **NeoBanco Microservicios**, siguiendo el formato ligero propuesto por Michael Nygard.

## ¿Qué es un ADR?

Un registro corto que explica una decisión de arquitectura: el contexto que la motivó, la decisión tomada y sus consecuencias (positivas y negativas). Sirve para que cualquier persona (incluido tu "yo" del futuro) entienda **por qué** el sistema está diseñado como está, no solo **qué** hace.

## Índice

| # | Título | Estado |
|---|--------|--------|
| [0001](0001-arquitectura-microservicios.md) | Arquitectura de microservicios en lugar de monolito | Aceptada |
| [0002](0002-autenticacion-jwt-stateless.md) | Autenticación mediante JWT stateless | Aceptada |
| [0003](0003-externalizacion-secreto-jwt.md) | Externalización del secreto de firma JWT | Aceptada |
| [0004](0004-common-security-modulo-independiente.md) | `common-security` como módulo Maven independiente (no monorepo) | Aceptada |
| [0005](0005-identificacion-usuario-entre-servicios.md) | Identificación del usuario entre microservicios mediante email (sin FK cruzada) | Aceptada |

## Formato de un ADR nuevo

```markdown
# N. Título corto de la decisión

## Estado
Propuesta | Aceptada | Rechazada | Reemplazada por ADR-X

## Contexto
¿Qué problema o fuerza motivó esta decisión?

## Decisión
¿Qué se decidió hacer, concretamente?

## Consecuencias
Qué mejora, qué costo o limitación se acepta a cambio.
```
