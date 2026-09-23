# Research: Permiso para aprobar documentos (HU-32)

No quedaron `NEEDS CLARIFICATION` en el Technical Context; estas son las decisiones de diseño
tomadas contra el código existente.

## R1. Representación en base de datos

- **Decision**: columna `can_approve BOOLEAN NOT NULL DEFAULT FALSE` en `users`, más
  `CONSTRAINT ck_users_can_approve_not_reader CHECK (NOT (role = 'READER' AND can_approve))`.
- **Rationale**: el `DEFAULT FALSE` cubre FR-001 (usuarios existentes sin permiso) sin backfill. El
  `CHECK` es defensa en profundidad de SC-002: ni un bug ni un `UPDATE` manual pueden dejar un
  lector aprobador. Consistente con el uso de `CHECK` para enums del proyecto.
- **Alternatives**: solo validar en servicio (descartada: no protege contra otras vías); tabla de
  permisos aparte (sobredimensionada para un flag).

## R2. Migración

- **Decision**: `V3__add_can_approve_to_users.sql` (V1 y V2 son las únicas existentes), con
  `ADD COLUMN IF NOT EXISTS` y `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT` para ser idempotente
  (Principio VI). No se modifica V1.
- **Rationale**: `ADD CONSTRAINT` no admite `IF NOT EXISTS` en PostgreSQL; el par drop/add lo
  simula. Al añadirla, todas las filas tienen `false`, así que el `CHECK` valida sin fallos.

## R3. Cómo se garantiza el efecto inmediato al retirar el permiso (FR-011)

- **Decision**: el permiso no entra al JWT. `isActiveApprover(userId)` consulta la base de datos en
  cada llamada con una única consulta derivada
  `existsByIdAndCanApproveTrueAndStatusAndRoleNot(id, ACTIVE, READER)`.
- **Rationale**: una sola consulta `exists` por PK, sin cargar la entidad, evalúa las tres
  condiciones (permiso, usuario activo, rol ≠ READER) contra el estado vigente. No se toca
  `tokenVersion`: incrementarlo cerraría la sesión completa, más de lo que pide la historia.
- **Alternatives**: `findById` + cálculo en memoria (una lectura más pesada, misma semántica);
  claim en el JWT (rechazada por FR-011); incrementar `tokenVersion` (efecto colateral
  indeseado: expulsa al usuario).
- **Nota**: usuario inexistente → `false` (no lanza excepción); el método es un predicado.

## R4. Reglas de create / update en `UserServiceImpl`

- **Decision** (todas al inicio del método, fail fast):
  - **Crear**: `canApprove` nulo → `false`. Rol READER con `true` → `BusinessRuleException(INVALID_ARGUMENT)`.
  - **Editar** (rol nuevo `R`, rol actual `C`, valor solicitado `v`, valor actual `a`):
    - `R == READER` y `C != READER` (cambio de rol a lector): resultado `false` sin error, aunque `v == true` (FR-005, Assumptions).
    - `R == READER` y `C == READER` y `v == true`: rechazo (US2 esc. 2).
    - resto: `v == null` → conserva `a` (FR-012); si no, `v`.
- **Rationale**: es exactamente la tabla de la spec. Se separan `resolveCanApproveOnCreate` y
  `resolveCanApproveOnUpdate` para no usar un flag booleano de bifurcación (Principio III).
- **Código de error**: `INVALID_ARGUMENT` (400) ya existe y encaja; evita extender el enum.
  Clave i18n: `user.can-approve.reader-not-allowed`.

## R5. Auditoría

- **Decision**: la entrada `EDIT_USER` existente ("Campos modificados: [...]") pasa a incluir
  `; can_approve: true → false` solo cuando el valor cambió. Si solo cambia el permiso, la lista
  de campos queda `[]` y la línea igual se registra. La creación (`CREATE_USER`) añade
  `; can_approve=true` solo cuando se crea con el permiso activo.
- **Rationale**: cumple FR-010/SC-003 sin acción nueva en `ActivityAction`. `ActivityLogService`
  (`REQUIRES_NEW`) no cambia.
- **Alternativa descartada**: entrada de bitácora separada por cambio de permiso: rompe "junto con el resto de la edición" (US3 esc. 3).

## R6. DTO de request y constitución (Principio IX)

- **Decision**: `@Nullable Boolean canApprove` + `@Schema` en ambos request DTO. Es una
  desviación consciente de la regla "toda componente con una restricción de
  `jakarta.validation.constraints`": FR-002/FR-012 exigen `null` como valor con significado
  (omitido) y no existe restricción que acote un `Boolean` nulable. Registrada en Complexity
  Tracking del plan y a declarar en el PR.

## R7. Exposición en respuestas

- **Decision**: `boolean canApprove` en `UserResponseDto` (detalle y listado), `CreateUserResponseDto`
  y `UpdateUserResponseDto`. No se añade a `UpdateStatusResponseDto` ni a `UserSummaryDto` (login):
  FR-008 solo pide creación, edición y consulta/listado, y añadirlo al login sugeriría que el
  cliente puede confiar en un valor de sesión (contradice FR-011).
- **MapStruct**: la entidad usa `boolean canApprove` (getter `isCanApprove()`); MapStruct lo mapea
  por nombre al componente `canApprove` del record sin `@Mapping`. Se verifica en `UserMapperTest`.

## R8. Autorización

- **Decision**: sin cambios. `UserController` ya restringe crear/editar/listar a ADMIN
  (`@PreAuthorize`); FR-003 se cubre con una prueba `@WebMvcTest`/de reglas existente ampliada.
  **A confirmar al implementar**: que los `@PreAuthorize` de esos endpoints siguen siendo
  `hasRole('ADMIN')`; si no, es un hallazgo aparte.

## R9. Alcance excluido

- Aprobar/devolver documentos, tabla de vistos buenos, UI, y aviso previo al guardado (el cliente
  ya recibe rol y `canApprove` actuales en `UserResponseDto`).
