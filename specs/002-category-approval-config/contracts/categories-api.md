# Contrato: API de categorías, campo `requiresApproval` (HU-31)

No hay endpoints nuevos: se extienden los existentes y las reglas de autorización no cambian.
Los ejemplos muestran solo lo que cambia.

## Campos nuevos

| Campo | Tipo JSON | Dónde | Presencia |
|-------|-----------|-------|-----------|
| `requiresApproval` | boolean | request de `POST` y `PUT` | opcional (`null`/omitido permitido) |
| `requiresApproval` | boolean | todas las respuestas de categoría (crear, editar, detalle, listado) | siempre |
| `approvalScopeNotice` | string \| null | respuesta de `PUT` | solo si el valor cambió |
| `approverWarning` | string \| null | respuestas de `POST` y `PUT` | solo si el valor pasó a `true` y hay menos de 2 aprobadores activos |

## `POST /api/categories`, crear (ADMIN)

Request (añade):

```json
{ "requiresApproval": true }
```

- Omitido o `null` → se crea con `false`.
- Rol distinto de ADMIN → **403**, mensaje "No tiene permisos para realizar esta acción".

Response `201` (añade):

```json
{
  "requiresApproval": true,
  "approverWarning": "Hay menos de dos usuarios con permiso de aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo"
}
```

`approverWarning` es `null` si se crea con `false` o si hay 2 o más aprobadores activos.

## `PUT /api/categories/{id}`, editar (ADMIN)

Request (añade): `"requiresApproval": true | false | null/omitido`.

| Situación | Resultado |
|-----------|-----------|
| Omitido / `null` | Conserva el valor; `approvalScopeNotice` y `approverWarning` en `null`; sin línea en bitácora. |
| Mismo valor que el actual | Igual que omitido. |
| `false → true` | Se guarda; `approvalScopeNotice` presente; `approverWarning` presente si hay menos de 2 aprobadores activos; bitácora `requires_approval: false → true`. |
| `true → false` | Se guarda; `approvalScopeNotice` presente; `approverWarning` en `null`; bitácora `requires_approval: true → false`. |
| Categoría inactiva | **403** (regla existente `category.inactive.cannot-edit`); nada cambia. |
| Rol distinto de ADMIN | **403** "No tiene permisos para realizar esta acción". |

Response `200` (añade):

```json
{
  "requiresApproval": true,
  "approvalScopeNotice": "Este cambio solo afecta a los documentos que se carguen desde ahora. Los documentos existentes conservan su estado actual",
  "approverWarning": null
}
```

## `GET /api/categories` y `GET /api/categories/{id}` (ADMIN, EDITOR)

`CategoryDetailResponseDto` añade `"requiresApproval": <boolean>`, también en cada elemento de
`categories` del listado. Sin cambios de autorización: EDITOR ya puede consultar.

## `PATCH /api/categories/{id}/status`

Sin cambios de contrato. Activar o desactivar una categoría no modifica `requiresApproval`.

## Contratos internos de servicio

```java
// category: nueva interfaz de solo lectura, sin dependencia de document
public interface CategoryQueryService {
    boolean requiresApproval(Long categoryId);
}

// user: método nuevo en la interfaz existente
long countActiveApprovers();
```

- `requiresApproval`: lee el valor vigente en cada llamada; categoría inexistente →
  `ResourceNotFoundException` (404, `category.not-found`). Pensado para la carga de documentos
  (HU-33), que debe invocarlo en cada carga, sin cachear.
- `countActiveApprovers`: número de usuarios con `can_approve = true`, `status = ACTIVE` y
  `role ≠ READER` (misma definición que `isActiveApprover`).

## Mensajes nuevos (`messages.properties`)

| Clave | Texto |
|-------|-------|
| `category.requires-approval.scope-notice` | Este cambio solo afecta a los documentos que se carguen desde ahora. Los documentos existentes conservan su estado actual |
| `category.requires-approval.few-approvers` | Hay menos de dos usuarios con permiso de aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo |
