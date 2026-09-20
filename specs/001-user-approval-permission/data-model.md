# Data Model: Permiso para aprobar documentos (HU-32)

## Entidad `User` (tabla `users`) — cambios

| Campo Java | Columna | Tipo | Restricciones | Notas |
|------------|---------|------|---------------|-------|
| `canApprove` | `can_approve` | `boolean` / `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Nuevo. `@Builder.Default = false`. |

Restricción de tabla nueva:

- `ck_users_can_approve_not_reader`: `NOT (role = 'READER' AND can_approve)`.

El resto de columnas no cambia. No se añade índice: `isActiveApprover` filtra por PK.

## Reglas de validación

| # | Regla | Dónde se aplica | Fuente |
|---|-------|-----------------|--------|
| V1 | Crear con `role = READER` y `canApprove = true` → error de regla de negocio | `UserServiceImpl.create` | FR-004 |
| V2 | Editar un usuario que ya es READER, con `canApprove = true` → error | `UserServiceImpl.update` | FR-004 |
| V3 | Editar cambiando el rol a READER → `canApprove` queda `false` (sin error, incluso si se envía `true`) | `UserServiceImpl.update` | FR-005 |
| V4 | `canApprove` omitido al crear → `false`; omitido al editar → conserva | servicio | FR-002, FR-012 |
| V5 | READER nunca con `can_approve = true` | `CHECK` en base de datos | SC-002 |

## Predicado derivado (no almacenado)

```text
isActiveApprover(userId) = can_approve AND status = 'ACTIVE' AND role <> 'READER'
```

Evaluado en cada llamada contra la base de datos (FR-011).

## Transiciones de estado de `canApprove`

| Evento | Antes → después | Bitácora |
|--------|-----------------|----------|
| Crear (sin marcar) | — → `false` | no |
| Crear EDITOR/ADMIN marcado | — → `true` | `CREATE_USER` incluye `can_approve=true` |
| Editar y marcar (no READER) | `false` → `true` | `EDIT_USER`: `can_approve: false → true` |
| Editar y desmarcar | `true` → `false` | `EDIT_USER`: `can_approve: true → false` |
| Cambio de rol a READER | `true` → `false` (automático) | `EDIT_USER`: `can_approve: true → false` |
| Editar sin cambiar el valor / campo omitido | sin cambio | no incluye la línea |
| Desactivar / reactivar usuario | sin cambio (valor conservado) | no cambia |

## Entradas de bitácora (`activity_log`)

Sin cambios de esquema ni de `ActivityAction`. Se reutiliza `EDIT_USER`; el campo `detail` gana el
fragmento `can_approve: [anterior] → [nuevo]` cuando el valor cambia.

## Fuera de alcance

Tabla de vistos buenos por documento (funcionalidad posterior): esta historia no la crea ni la
altera; retirar el permiso no toca ninguna otra tabla (FR-009).
