# Quickstart: validar HU-32

## Prerrequisitos

- Java 17, wrapper Maven (`./mvnw`), base PostgreSQL local para el perfil `local`.
- Un usuario ADMIN y un token de sesión suyo (`POST /auth/login`).

## 1. Puerta automática

```bash
./mvnw clean verify
```

Debe terminar en verde con los umbrales JaCoCo (≥80% líneas, ≥65% ramas). Las pruebas clave, por
nombre (`<accion>_<contexto>_<resultado>`), cubren:

| Historia | Clase de prueba | Qué prueba |
|----------|-----------------|------------|
| US1 | `UserServiceTest` | crear sin marcar → `false`; EDITOR marcado → `true`; ADMIN sin permiso → `false`; edición marcando → bitácora `can_approve: false → true` |
| US2 | `UserServiceTest`, `UserControllerWebMvcTest` | crear/editar READER con `true` → 400 y sin guardado |
| US3 | `UserServiceTest` | EDITOR aprobador → READER: queda `false`, con `true` enviado también; bitácora `true → false` |
| US4 | `UserMapperTest` | `canApprove` presente en respuestas de detalle/listado/creación/edición |
| US5 | `UserServiceTest` | `isActiveApprover`: falso si inactivo, READER o sin permiso; verdadero si cumple las tres; desactivar/reactivar conserva el valor |
| Bordes | `UserServiceTest` | campo omitido conserva; sin cambio → sin línea de bitácora; READER ya en `false` sin tocar → sin registro |

## 2. Migración

Arrancar con `./mvnw spring-boot:run` (perfil local). Flyway debe aplicar `V3` sin errores.
Comprobar: todos los usuarios existentes con `can_approve = false`; un
`UPDATE users SET can_approve = true WHERE role = 'READER'` debe fallar por
`ck_users_can_approve_not_reader`.

## 3. Recorrido manual (API)

1. `POST /api/users` con rol `EDITOR` y `"canApprove": true` → 201 con `canApprove: true`.
2. `GET /api/users` → ese usuario con `canApprove: true`; ADMIN con `false`.
3. `POST /api/users` con rol `READER` y `"canApprove": true` → 400, mensaje traducido.
4. `PUT /api/users/{id}` de ese EDITOR cambiando `role` a `READER` → 200 con `canApprove: false`.
5. Consultar la bitácora de actividad (o la tabla `activity_log`) → entrada `EDIT_USER` con `can_approve: true → false`.
6. Desactivar y reactivar un aprobador (`changeStatus`) → `canApprove` conserva su valor.
7. Sesión inmediata: con un EDITOR aprobador logueado, retirarle el permiso; `isActiveApprover`
   devuelve `false` de inmediato (validado en pruebas del servicio; no requiere re-login).

## Resultado esperado

Todos los criterios SC-001 a SC-006 verificables con lo anterior; el `CHANGELOG.md` incluye la
entrada `Added` bajo `[Unreleased]` en el mismo PR.
