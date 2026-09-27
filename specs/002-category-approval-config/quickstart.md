# Quickstart: validar HU-31

## Prerrequisitos

- Java 17, wrapper Maven (`./mvnw`), base PostgreSQL local para el perfil `local`.
- Un usuario ADMIN y un EDITOR con sus tokens (`POST /auth/login`).
- Para la advertencia: conocer cuántos aprobadores activos hay (HU-32, `canApprove`).

## 1. Puerta automática

```bash
./mvnw clean verify
```

Debe terminar en verde con los umbrales JaCoCo (≥80% líneas, ≥65% ramas). Pruebas clave
(`<accion>_<contexto>_<resultado>`):

| Historia | Clase de prueba | Qué prueba |
|----------|-----------------|------------|
| US1 | `CategoryServiceTest` | crear sin campo → `false`; crear con `true` → `true`; editar `false → true`; editar omitiendo el campo conserva el valor |
| US1 | `CategoryControllerWebMvcTest` | `requiresApproval` viaja en request/response; `create` y `update` anotados con `hasRole('ADMIN')` |
| US2 | `CategoryServiceTest` | cambio de valor → `approvalScopeNotice` y línea `requires_approval: a → b` en `EDIT_CATEGORY`; sin cambio → ni aviso ni línea; cambiar solo el indicador no llama a `DocumentCommandService` |
| US2 | `CategoryServiceTest` | `CREATE_CATEGORY` incluye `(requires_approval: <valor>)` |
| US3 | `CategoryServiceTest` | 0 y 1 aprobadores + activar → advertencia; 2 → sin advertencia; desactivar o sin cambio → no consulta el conteo |
| US3 | `UserServiceTest` | `countActiveApprovers` delega en la consulta derivada con `ACTIVE` y `READER` |
| US4 | `CategoryMapperTest` | `requiresApproval` en detalle, listado, creación y edición; aviso y advertencia mapeados desde `ApprovalNotices` |
| FR-014 | `CategoryQueryServiceTest` | `requiresApproval` devuelve el valor guardado; categoría inexistente → `ResourceNotFoundException` |

## 2. Migración

Arrancar con `./mvnw spring-boot:run` (perfil local). Flyway debe aplicar `V4` sin errores y
`GET /api/categories` debe devolver las 8 categorías sembradas con `requiresApproval: false`.

## 3. Recorrido manual (API)

1. Como ADMIN, `POST /api/categories` sin `requiresApproval` → 201 con `requiresApproval: false`.
2. Con menos de 2 aprobadores activos, `PUT /api/categories/{id}` de "Actas" con
   `"requiresApproval": true` → 200 con `requiresApproval: true`, `approvalScopeNotice` y
   `approverWarning` presentes.
3. Designar un segundo aprobador (HU-32) y activar "Resoluciones" → 200 sin `approverWarning`.
4. Repetir el `PUT` de "Actas" sin el campo → 200, `requiresApproval` sigue en `true`, sin aviso.
5. Revisar `activity_log`: `EDIT_CATEGORY` con `requires_approval: false → true` para Actas; la
   edición sin cambio no incluye la línea.
6. Como EDITOR, `GET /api/categories` → 200 con `requiresApproval` en cada categoría.
7. Como EDITOR, `PUT /api/categories/{id}` → 403 "No tiene permisos para realizar esta acción".
8. Comprobar que los documentos existentes de "Actas" no cambiaron tras el paso 2.

## Resultado esperado

SC-001 a SC-008 verificables con lo anterior. `CHANGELOG.md` incluye la entrada `Added` bajo
`[Unreleased]` en el mismo PR.
