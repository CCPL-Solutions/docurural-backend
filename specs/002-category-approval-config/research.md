# Research: Configuración de aprobación por categoría (HU-31)

No quedaron `NEEDS CLARIFICATION` en el Technical Context; estas son las decisiones de diseño
tomadas contra el código existente.

## R1. Representación en base de datos y migración

- **Decision**: columna `requires_approval BOOLEAN NOT NULL DEFAULT FALSE` en `categories`, en la
  migración nueva `V4__add_requires_approval_to_categories.sql` con `ADD COLUMN IF NOT EXISTS`.
- **Rationale**: el `DEFAULT FALSE` deja las 8 categorías sembradas en `false` sin backfill
  (FR-012, SC-007). V1–V3 no se tocan (Principio VI). No hace falta `CHECK`: el booleano no tiene
  estados inválidos ni depende de otra columna.
- **Alternatives**: tabla de configuración de flujo por categoría (sobredimensionada para un
  indicador); `UPDATE` explícito a `false` (redundante con el `DEFAULT`).

## R2. Semántica del campo en request (crear / editar)

- **Decision**: `@Nullable Boolean requiresApproval` en `CreateCategoryRequestDto` y
  `UpdateCategoryRequestDto`.
  - Crear: `null` → `false` (FR-002).
  - Editar: `null` → conserva el valor actual y cuenta como "sin cambio" (FR-003, clarificación 2).
- **Rationale**: se necesitan tres estados (`true`, `false`, omitido). Es la misma decisión que
  `canApprove` en HU-32 y mantiene compatible al cliente web actual.
- **Nota constitucional (Principio IX)**: ninguna restricción de Bean Validation acota un
  `Boolean` nulable; `@NotNull` contradiría FR-002/FR-003. Se declara `@Nullable` y se registra en
  Complexity Tracking, igual que en HU-32.

## R3. Consulta reutilizable `requiresApproval(categoryId)` (FR-014)

- **Decision**: nueva interfaz de solo lectura `CategoryQueryService` con
  `boolean requiresApproval(Long categoryId)`, implementada en `CategoryQueryServiceImpl`, que
  depende solo de `CategoryRepository` y `MessageResolver`. Lee con `findById` en cada llamada;
  si no existe lanza `ResourceNotFoundException` con `category.not-found`.
- **Rationale**: la clarificación pidió el método en `CategoryService`, pero el consumidor previsto
  es el módulo `document` (HU-33), y la desviación D-4 de la constitución advierte que inyectar
  `CategoryService` en `document` cierra un ciclo de Spring, porque `CategoryServiceImpl` ya
  inyecta `DocumentCommandService` y `DocumentQueryService`. La propia D-4 prescribe la solución:
  "extraer una interfaz de solo lectura de `category` que no dependa de `document`". Crear esa
  interfaz ahora entrega el contrato de FR-014 sin ciclo y deja el terreno listo para corregir D-4
  más adelante. La spec (FR-014) se ajustó para nombrar esta interfaz.
- **Alternatives**: método en `CategoryService` (rechazada: HU-33 no podría usarlo sin ciclo o sin
  `@Lazy`, que la constitución descarta); consulta desde `document` por `CategoryRepository`
  (rechazada: agrava D-4).
- **Nota**: ya no hay caché de categorías, así que "valor vigente" (SC-008) se cumple con la
  lectura directa.

## R4. Conteo de aprobadores activos (FR-009, FR-010)

- **Decision**: nuevo método `long countActiveApprovers()` en `UserService`, implementado con la
  consulta derivada `countByCanApproveTrueAndStatusAndRoleNot(UserStatus.ACTIVE, UserRole.READER)`
  en `UserRepository`. `CategoryServiceImpl` lo usa a través de la interfaz.
- **Rationale**: misma definición de aprobador activo que `isActiveApprover` (HU-32), en una sola
  consulta `count`. `UserServiceImpl` no depende de `category` ni de `document`, así que no hay
  ciclo. Respeta el Principio I (no se consulta `users` desde `category`).
- **Alternatives**: contar en `category` vía `UserRepository` (viola Principio I); iterar
  `isActiveApprover` sobre todos los usuarios (N consultas).
- **Cuándo se consulta**: solo cuando el indicador *pasa* a `true` (creación con `true`, o edición
  `false → true`). En el resto de guardados no se hace la consulta.

## R5. Aviso de alcance y advertencia en la respuesta (FR-007, FR-009)

- **Decision**: dos campos nuevos, nulables, en las respuestas de guardado:
  - `CreateCategoryResponseDto.approverWarning` (String).
  - `UpdateCategoryResponseDto.approvalScopeNotice` y `UpdateCategoryResponseDto.approverWarning`
    (String).
  Son `null` cuando no aplican. Los textos se resuelven con `MessageResolver` sobre las claves
  `category.requires-approval.scope-notice` y `category.requires-approval.few-approvers`.
- **Rationale**: el cliente distingue un aviso informativo de una advertencia sin interpretar
  textos; el guardado sigue devolviendo 201/200 (advertencia no bloqueante). Se sigue el patrón
  ya existente de `message` en esas respuestas. La creación no lleva aviso de alcance (no hay
  documentos previos).
- **Límite de parámetros (Principio III)**: `toUpdateResponse` pasaría a 4 parámetros; se agrupan
  el aviso y la advertencia en un `record ApprovalNotices(String scopeNotice, String approverWarning)`
  del paquete `category.dto`, así el mapper queda en `(category, message, notices)`.
- **Alternatives**: lista genérica `warnings` (mezcla aviso y advertencia, el cliente tendría que
  distinguirlos por texto); endpoint aparte para consultar aprobadores (dos llamadas para un caso
  simple).

## R6. Auditoría (FR-011)

- **Decision**:
  - **Editar**: la lista `modifiedFields` que ya alimenta `EDIT_CATEGORY` ("Campos modificados:
    [...]") añade `requires_approval: false → true` solo cuando el valor cambia, igual que hoy se
    añade `defaultSensitivityLevel: A → B`. Se registra en la misma entrada que el resto de la
    edición.
  - **Crear**: el detalle de `CREATE_CATEGORY` pasa a
    `Categoria creada: <nombre> (requires_approval: <valor>)`, siempre (clarificación 3).
- **Rationale**: sin acciones nuevas en `ActivityAction` y sin tocar `ActivityLogService`
  (`REQUIRES_NEW` intacto, Principio VIII). El literal del detalle sigue el estilo actual, que ya
  construye esos textos en el servicio.

## R7. Autorización y lectura por EDITOR (FR-004, FR-006)

- **Decision**: sin cambios en `CategoryController` ni en `SecurityConfig`. `POST` y `PUT` ya son
  `@PreAuthorize("hasRole('ADMIN')")`; `GET /categories` y `GET /categories/{id}` ya admiten
  `ADMIN` y `EDITOR`. El 403 lo produce el manejo de acceso denegado existente con la clave
  `auth.access-denied` ("No tiene permisos para realizar esta acción").
- **Rationale**: el criterio 2 y la nota técnica de EDITOR ya se cumplen; solo se añade el campo a
  las respuestas de lectura. Como `@WebMvcTest` excluye `SecurityConfig`, se verifica la regla con
  una prueba que comprueba las anotaciones `@PreAuthorize` de `create` y `update`.

## R8. Documentos existentes (FR-008)

- **Decision**: cambiar el indicador no invoca a `DocumentCommandService` (a diferencia de subir la
  sensibilidad, que sí reclasifica). Se prueba con `verifyNoInteractions` sobre el servicio de
  documentos cuando solo cambia el indicador.
- **Rationale**: los estados Borrador/En revisión aún no existen (`DocumentStatus` = ACTIVE/DELETED);
  no tocar documentos garantiza FR-008 hoy y cuando existan.

## Observación fuera de alcance

`CategoryServiceImpl.create` usa `UserRepository.getReferenceById` para fijar `createdBy`, lo que
es un acceso cruzado de repositorio (Principio I) que no figura en la desviación D-4. No se corrige
en esta historia; se sugiere registrarlo en D-4.
