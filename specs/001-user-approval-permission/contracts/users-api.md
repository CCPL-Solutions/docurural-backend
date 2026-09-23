# Contrato: API de usuarios — campo `canApprove` (HU-32)

No hay endpoints nuevos. Se extienden los existentes. Todos requieren rol **ADMIN** (sin cambios).
Los ejemplos muestran solo lo que cambia.

## Campo nuevo

| Campo | Tipo JSON | En request | En response |
|-------|-----------|------------|-------------|
| `canApprove` | boolean | opcional (`null`/omitido permitido) | siempre presente |

## `POST /api/users` — crear

Request (añade):

```json
{ "canApprove": true }
```

- Omitido o `null` → se crea con `false`.
- `role = READER` con `canApprove = true` → **400** `INVALID_ARGUMENT`, mensaje
  `user.can-approve.reader-not-allowed`; no se guarda nada.

Response `201`: añade `"canApprove": <boolean>`.

## `PUT /api/users/{id}` — editar

Request (añade): `"canApprove": true | false | null/omitido`.

| Situación | Resultado |
|-----------|-----------|
| Omitido / `null` | Conserva el valor actual (salvo cambio de rol a READER: pasa a `false`). |
| `true`/`false` en ADMIN o EDITOR | Se aplica. Si cambia, `EDIT_USER` registra `can_approve: a → b`. |
| Rol nuevo READER, rol actual distinto de READER | `canApprove` queda `false`, **200**, aunque se envíe `true`. |
| Rol actual y nuevo READER, `canApprove = true` | **400** `INVALID_ARGUMENT`; sin cambios guardados. |

Response `200`: añade `"canApprove": <boolean>` (valor ya efectivo tras el retiro automático).

## `GET /api/users` y `GET /api/users/{id}`

`UserResponseDto` añade `"canApprove": <boolean>`. En el listado, el cliente muestra la etiqueta
"Aprobador" donde sea `true`. Los usuarios existentes devuelven `false`.

## Cambio de estado de usuario (`changeStatus`)

Sin cambios de contrato. Desactivar conserva el valor guardado de `canApprove`.

## Contrato interno de servicio

```java
// UserService
boolean isActiveApprover(Long userId);
```

- Devuelve `true` solo si `can_approve` es verdadero, `status = ACTIVE` y `role ≠ READER`, leyendo
  el estado vigente de la base de datos en cada llamada.
- Usuario inexistente → `false`. Nunca lanza por ausencia y nunca devuelve `null`.
- Contrato para consumidores futuros (aprobar/devolver documentos): deben invocarlo en cada
  operación, sin cachear el resultado ni derivarlo del token.
