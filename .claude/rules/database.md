---
paths:
  - "src/main/java/**/repository/**"
  - "src/main/resources/db/migration/**"
---

# Base de Datos y Migraciones

## Migraciones Flyway

Ubicación: `src/main/resources/db/migration/`

## Restricciones críticas

- **Nunca modificar** migraciones existentes. Flyway detecta el checksum y falla el arranque. Si hay un error, crear una
  nueva migración correctiva.
- Para cualquier cambio de esquema: crear `V(n+1)__{descripcion_con_guiones_bajos}.sql`.
- Nunca usar `DELETE` SQL en lógica de negocio — solo marcar estado (ver Soft deletes).
- Preferir `IF NOT EXISTS` / `IF EXISTS` para hacer las migraciones idempotentes.

## Soft deletes

| Entidad      | Campo    | Estados               |
|--------------|----------|-----------------------|
| `users`      | `status` | `ACTIVE` / `INACTIVE` |
| `categories` | `status` | `ACTIVE` / `INACTIVE` |
| `documents`  | `status` | `ACTIVE` / `DELETED`  |

Los registros eliminados nunca se borran físicamente. Esto preserva la integridad referencial en `activity_log`.

## Consultas personalizadas

Preferir métodos derivados de Spring Data (`findBySkuAndActiveTrue`) sobre JPQL para
consultas simples. Usar `@Query` con JPQL solo cuando la consulta derivada sea ilegible.
Usar SQL nativo (`nativeQuery = true`) solo si JPQL no es suficiente; documentar el motivo.
