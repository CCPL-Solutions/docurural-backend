---

description: "Lista de tareas para HU-32 — Permiso para aprobar documentos"
---

# Tasks: Permiso para aprobar documentos (HU-32)

**Input**: Documentos de diseño en `/specs/001-user-approval-permission/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/users-api.md, quickstart.md

**Tests**: INCLUIDAS. No son opcionales aquí: la constitución (Principios IV y V) exige pruebas por capa, fixtures de `TestFixtures` y cobertura JaCoCo ≥80% líneas / ≥65% ramas.

**Organization**: Tareas agrupadas por historia de usuario. Todo el código vive en el paquete `user` (`src/main/java/co/edu/docurural/user/`); no se crean clases de producción nuevas.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede ejecutar en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1..US5)
- Nombres de prueba: `<accion>_<contexto>_<resultado>`; objetos de dominio solo con builders de `TestFixtures`

## Path Conventions

- Producción: `src/main/java/co/edu/docurural/user/{entity,dto,mapper,repository,service,controller}/`
- Recursos: `src/main/resources/`
- Pruebas: `src/test/java/co/edu/docurural/user/{service,controller,mapper,dto}/`, fixtures en `src/test/java/co/edu/docurural/support/TestFixtures.java`

---

## Phase 1: Setup

**Purpose**: verificar el punto de partida

- [X] T001 Confirmar línea base verde ejecutando `./mvnw clean verify` en la raíz del repo y confirmar que en `src/main/java/co/edu/docurural/user/controller/UserController.java` los endpoints de crear, editar y listar usuarios están protegidos con `@PreAuthorize("hasRole('ADMIN')")` (R8 del research; si no lo están, anotarlo como hallazgo aparte, no corregirlo aquí)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: columna, entidad, DTOs, mensaje y fixtures que TODAS las historias necesitan

**⚠️ CRITICAL**: ninguna historia puede empezar hasta terminar esta fase

- [X] T002 [P] Crear `src/main/resources/db/migration/V3__add_can_approve_to_users.sql` (NO tocar V1/V2): `ALTER TABLE users ADD COLUMN IF NOT EXISTS can_approve BOOLEAN NOT NULL DEFAULT FALSE;` seguido de `ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_can_approve_not_reader;` y `ALTER TABLE users ADD CONSTRAINT ck_users_can_approve_not_reader CHECK (NOT (role = 'READER' AND can_approve));`
- [X] T003 [P] Añadir a `src/main/java/co/edu/docurural/user/entity/User.java` el campo `private boolean canApprove;` con `@Builder.Default` = `false` y `@Column(name = "can_approve", nullable = false)` (columna `can_approve` `BOOLEAN NOT NULL DEFAULT FALSE`)
- [X] T004 [P] Añadir el componente `@Nullable Boolean canApprove` (sin anotación de `jakarta.validation.constraints`; null = omitido, FR-002/FR-012) con `@Schema` descriptivo en `src/main/java/co/edu/docurural/user/dto/CreateUserRequestDto.java` y en `src/main/java/co/edu/docurural/user/dto/UpdateUserRequestDto.java`, y actualizar todos los puntos que construyen estos records (servicio, pruebas existentes, `TestFixtures`) para que compilen. Ver Complexity Tracking del plan (declarar en el PR)
- [X] T005 [P] Añadir el componente `boolean canApprove` a `src/main/java/co/edu/docurural/user/dto/UserResponseDto.java`, `src/main/java/co/edu/docurural/user/dto/CreateUserResponseDto.java` y `src/main/java/co/edu/docurural/user/dto/UpdateUserResponseDto.java` (NO añadir a `UpdateStatusResponseDto` ni al DTO de login, R7); corregir los constructores/usos existentes. `UserMapper` no cambia: MapStruct mapea por nombre
- [X] T006 [P] Añadir en `src/main/resources/messages.properties` la clave `user.can-approve.reader-not-allowed=Los lectores no pueden aprobar documentos` junto a las demás claves `user.*`
- [X] T007 [P] En `src/test/java/co/edu/docurural/support/TestFixtures.java` añadir `userApprover(Long id)` (EDITOR activo con `canApprove(true)`), un builder/variante `userReader(Long id)` si no existe, y helpers de request `createUserRequest(role, canApprove)` y `updateUserRequest(role, canApprove)` que acepten `Boolean` nulable, siguiendo el estilo de los builders existentes
- [X] T008 Verificar que `./mvnw clean verify` compila y las pruebas existentes siguen verdes con los cambios de T002–T007 (Flyway aplica V3 en el perfil de prueba)

**Checkpoint**: modelo, DTOs y fixtures listos; las historias pueden empezar

---

## Phase 3: User Story 1 - Otorgar el permiso al crear o editar (Priority: P1) 🎯 MVP

**Goal**: el administrador crea/edita usuarios enviando `canApprove`; omitido al crear → `false`, omitido al editar → conserva; ADMIN no lo obtiene implícitamente; el cambio queda en bitácora.

**Independent Test**: crear EDITOR con `canApprove: true` → se guarda y se devuelve `true`; editar un ADMIN sin marcar → sigue `false`; editar EDITOR marcando → `EDIT_USER` con `can_approve: false → true`.

### Tests for User Story 1

> Escribirlas primero y comprobar que FALLAN antes de implementar

- [X] T009 [P] [US1] En `src/test/java/co/edu/docurural/user/service/UserServiceTest.java` añadir: `create_setsCanApproveFalse_whenFieldOmitted`, `create_persistsCanApproveTrue_whenRoleIsEditorAndFlagTrue`, `create_logsCanApprove_whenCreatedWithPermission` (el detalle de `CREATE_USER` incluye `can_approve=true`), `update_setsCanApproveTrueAndLogsChange_whenEditorFlagged` (detalle contiene `can_approve: false → true`), `update_keepsCanApprove_whenFieldOmitted`, `update_doesNotLogCanApprove_whenValueUnchanged`, `update_logsCanApproveLine_whenOnlyPermissionChanges` (lista de campos `[]` y línea igual registrada), `create_leavesAdminWithoutPermission_whenFlagNotSent`
- [X] T010 [P] [US1] En `src/test/java/co/edu/docurural/user/controller/UserControllerWebMvcTest.java` añadir pruebas de `POST /api/users` y `PUT /api/users/{id}` con `canApprove` en request y verificar `canApprove` en el JSON de respuesta (`create_returns201WithCanApprove_whenEditorFlagged`, `update_returns200WithCanApprove_whenFlagSent`), siguiendo la plantilla `@WebMvcTest` de CLAUDE.md
- [X] T011 [P] [US1] En `src/test/java/co/edu/docurural/user/dto/UpdateUserRequestDtoTest.java` añadir prueba de que `canApprove` nulo es válido (`validate_passes_whenCanApproveIsNull`) y `true`/`false` también

### Implementation for User Story 1

- [X] T012 [US1] En `src/main/java/co/edu/docurural/user/service/UserServiceImpl.java` implementar `private boolean resolveCanApproveOnCreate(CreateUserRequestDto request)` (null → `false`; sin flags booleanos de bifurcación, Principio III) y usarlo al construir la entidad en `create`
- [X] T013 [US1] En `UserServiceImpl.update` implementar `private boolean resolveCanApproveOnUpdate(User current, UpdateUserRequestDto request)` para el caso base (null → conserva valor actual; si no, el valor enviado) y aplicarlo a la entidad; máximo 3 parámetros por método (depende de T012, mismo archivo)
- [X] T014 [US1] En `UserServiceImpl` extender el detalle de auditoría: `EDIT_USER` añade `; can_approve: <anterior> → <nuevo>` solo si el valor cambió (si solo cambió el permiso, la lista de campos queda `[]` y la línea igual se registra); `CREATE_USER` añade `; can_approve=true` solo cuando se crea con el permiso activo (R5). Un log `info` por operación exitosa siguiendo el estilo existente
- [X] T015 [US1] Ejecutar `./mvnw test -Dtest=UserServiceTest,UserControllerWebMvcTest,UpdateUserRequestDtoTest` y confirmar que T009–T011 pasan

**Checkpoint**: US1 funcional y comprobable por sí sola (MVP)

---

## Phase 4: User Story 2 - Los lectores no pueden ser aprobadores (Priority: P1)

**Goal**: el servidor rechaza crear o editar un READER con el permiso activo, sin guardar nada.

**Independent Test**: `POST` con rol READER y `canApprove: true` → 400 `INVALID_ARGUMENT` con el mensaje traducido; `PUT` sobre un READER existente con `true` → 400.

### Tests for User Story 2

- [X] T016 [P] [US2] En `UserServiceTest.java` añadir: `create_throwsBusinessRule_whenReaderWithCanApproveTrue` (verificar `BusinessErrorCode.INVALID_ARGUMENT`, clave `user.can-approve.reader-not-allowed` y `verify(userRepository, never()).save(any())`), `update_throwsBusinessRule_whenReaderStaysReaderWithCanApproveTrue` (sin guardado), `create_persistsCanApprove_whenAdminFlagged`, `create_acceptsReader_whenCanApproveFalseOrOmitted`
- [X] T017 [P] [US2] En `UserControllerWebMvcTest.java` añadir `create_returns400_whenReaderWithCanApproveTrue` y `update_returns400_whenReaderWithCanApproveTrue` verificando el cuerpo de error del `GlobalExceptionHandler`

### Implementation for User Story 2

- [X] T018 [US2] En `UserServiceImpl.create` (al inicio del método, fail fast) lanzar `BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, ...)` con el mensaje resuelto por `MessageResolver` de la clave `user.can-approve.reader-not-allowed` cuando el rol sea `READER` y `canApprove` sea `true`; NO modificar `GlobalExceptionHandler` (Principio VIII). Extraer el chequeo a un método privado reutilizable para T019 (regla DRY)
- [X] T019 [US2] En `UserServiceImpl.update`, dentro de `resolveCanApproveOnUpdate`, lanzar el mismo error cuando el rol actual y el nuevo sean `READER` y el valor enviado sea `true`, antes de mutar la entidad
- [X] T020 [US2] Ejecutar `./mvnw test -Dtest=UserServiceTest,UserControllerWebMvcTest` y confirmar T016–T017 verdes

**Checkpoint**: US1 y US2 funcionan; ningún READER queda con el permiso vía API

---

## Phase 5: User Story 3 - Retiro automático al pasar a lector (Priority: P2)

**Goal**: cambiar el rol de un usuario a READER retira el permiso en el mismo guardado, aunque se envíe `true`, sin error, y queda en bitácora.

**Independent Test**: EDITOR con permiso → `PUT` con `role: READER` → 200, `canApprove: false`, bitácora `can_approve: true → false`.

### Tests for User Story 3

- [X] T021 [P] [US3] En `UserServiceTest.java` añadir: `update_removesCanApprove_whenRoleChangesToReader`, `update_removesCanApproveWithoutError_whenRoleChangesToReaderAndFlagTrueSent`, `update_logsCanApproveTrueToFalse_whenRoleChangesToReader`, `update_doesNotLogCanApprove_whenReaderAlreadyFalseAndFieldOmitted` (READER con `false` editado sin tocar la casilla → sin registro)
- [X] T022 [P] [US3] En `UserControllerWebMvcTest.java` añadir `update_returns200WithCanApproveFalse_whenRoleChangesToReader` (el servicio mockeado devuelve la respuesta efectiva; verificar que el JSON expone `canApprove: false`)

### Implementation for User Story 3

- [X] T023 [US3] En `UserServiceImpl.resolveCanApproveOnUpdate` añadir la rama "rol nuevo READER y rol actual distinto de READER → resultado `false` sin error, incluso si el valor enviado es `true`" (FR-005); comprobar el orden de ramas contra la tabla de R4 del research; la auditoría de T014 ya registra `true → false`
- [X] T024 [US3] Ejecutar `./mvnw test -Dtest=UserServiceTest,UserControllerWebMvcTest` y confirmar T021–T022 verdes

**Checkpoint**: la regla de integridad de READER es completa (crear, editar, cambio de rol)

---

## Phase 6: User Story 4 - Ver aprobadores en el listado (Priority: P2)

**Goal**: detalle, listado, creación y edición devuelven `canApprove` para que el cliente muestre la etiqueta "Aprobador".

**Independent Test**: mapear usuarios con y sin permiso y verificar el valor en cada DTO de respuesta.

### Tests for User Story 4

- [X] T025 [P] [US4] En `src/test/java/co/edu/docurural/user/mapper/UserMapperTest.java` (instanciar con `Mappers.getMapper(UserMapper.class)`) añadir pruebas de que `canApprove` se mapea a `UserResponseDto`, `CreateUserResponseDto` y `UpdateUserResponseDto` tanto en `true` (`userApprover`) como en `false`, y que `passwordHash` sigue ausente
- [X] T026 [P] [US4] En `UserControllerWebMvcTest.java` añadir `list_returnsCanApprove_forEachUser` y `getById_returnsCanApprove` sobre `GET /api/users` y `GET /api/users/{id}` con un aprobador y un no aprobador
- [X] T027 [P] [US4] En `UserServiceTest.java` añadir `findAll_exposesCanApprove_forApproverAndNonApprover` (con `@Spy` del mapper real)

### Implementation for User Story 4

- [X] T028 [US4] Si T025 falla por algún mapeo, corregir `src/main/java/co/edu/docurural/user/mapper/UserMapper.java` (esperado: sin cambios, mapeo por nombre desde `boolean canApprove`); en cualquier caso, ejecutar `./mvnw test -Dtest=UserMapperTest,UserControllerWebMvcTest,UserServiceTest` y confirmar T025–T027 verdes

**Checkpoint**: la etiqueta "Aprobador" tiene el dato necesario en todas las respuestas pedidas

---

## Phase 7: User Story 5 - Aprobador activo, ciclo de vida y efecto inmediato (Priority: P2)

**Goal**: `UserService.isActiveApprover(userId)` consulta la BD en cada llamada (permiso + ACTIVE + rol ≠ READER); desactivar/reactivar conserva el valor; retirar el permiso surte efecto inmediato; el permiso no viaja en el JWT.

**Independent Test**: `isActiveApprover` falso para inactivo/READER/sin permiso/inexistente y verdadero cuando cumple las tres condiciones; `changeStatus` no altera `canApprove`.

### Tests for User Story 5

- [X] T029 [P] [US5] En `UserServiceTest.java` añadir: `isActiveApprover_returnsTrue_whenPermissionActiveAndUserActiveAndNotReader`, `isActiveApprover_returnsFalse_whenRepositoryReportsNoMatch` (cubre inactivo, READER, sin permiso e inexistente mockeando `existsByIdAndCanApproveTrueAndStatusAndRoleNot(id, UserStatus.ACTIVE, UserRole.READER)`), `isActiveApprover_queriesRepositoryOnEveryCall` (`verify(..., times(2))` tras dos llamadas consecutivas: sin caché), `changeStatus_keepsCanApprove_whenDeactivatedAndReactivated`, `update_isActiveApproverFalseImmediately_afterPermissionRemoved`
- [X] T030 [P] [US5] Comprobar por prueba que el permiso no está en el token: en la prueba existente del generador/servicio de JWT (localizar con `Grep "tokenVersion"` bajo `src/test`) añadir un caso que verifique que los claims emitidos no contienen `canApprove`; si no existe una clase de prueba adecuada, crear `src/test/java/co/edu/docurural/auth/...` siguiendo la convención `{ClaseTesteada}Test`

### Implementation for User Story 5

- [X] T031 [US5] En `src/main/java/co/edu/docurural/user/repository/UserRepository.java` añadir el método derivado `boolean existsByIdAndCanApproveTrueAndStatusAndRoleNot(Long id, UserStatus status, UserRole role);` (sin `@Query`, Principio VI)
- [X] T032 [US5] En `src/main/java/co/edu/docurural/user/service/UserService.java` declarar `boolean isActiveApprover(Long userId);` con Javadoc del contrato (true solo si permiso activo, usuario ACTIVE y rol ≠ READER; inexistente → `false`; los consumidores deben invocarlo en cada operación sin cachear ni derivarlo del token) y en `UserServiceImpl.java` implementarlo delegando en el repositorio con `UserStatus.ACTIVE` y `UserRole.READER`; no modificar `tokenVersion`
- [X] T033 [US5] Verificar que `changeStatus` en `UserServiceImpl.java` no toca `canApprove` (solo lectura del código; ajustar solo si algo lo modifica) y ejecutar `./mvnw test -Dtest=UserServiceTest` confirmando T029–T030 verdes

**Checkpoint**: todas las historias completas; punto de verificación listo para el flujo de aprobación posterior

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T034 [P] Añadir bajo `## [Unreleased]` → `### Added` de `CHANGELOG.md` una entrada (HU-32): indicador `canApprove` en usuarios, restricción para READER con retiro automático, auditoría `can_approve: a → b`, `UserService.isActiveApprover`, migración `V3`, y su exposición en las respuestas de usuarios
- [X] T035 [P] Revisar `src/main/java/co/edu/docurural/user/service/UserServiceImpl.java` contra CLAUDE.md: métodos ≤3 parámetros, métodos que caben en pantalla, sin código comentado, nombres que revelan intención; refactorizar helpers privados si `create`/`update` crecieron demasiado
- [X] T036 Ejecutar `./mvnw clean verify` y confirmar verde con los umbrales JaCoCo (≥80% líneas, ≥65% ramas) sin añadir exclusiones nuevas
- [X] T037 Recorrer `specs/001-user-approval-permission/quickstart.md`: comprobar la migración (`ck_users_can_approve_not_reader` rechaza `UPDATE users SET can_approve = true WHERE role = 'READER'`) y el recorrido manual de la API (pasos 1–7)
- [X] T038 Preparar la descripción del PR incluyendo la desviación de Principio IX (`@Nullable Boolean canApprove` sin restricción de `jakarta.validation.constraints`) tal como se registró en Complexity Tracking del plan

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA todas las historias
- **User Stories (Phases 3–7)**: dependen de Foundational. Todas modifican `UserServiceImpl.java` y `UserServiceTest.java`, por lo que sus tareas de servicio deben ir en secuencia, no en paralelo
- **Polish (Phase 8)**: depende de las historias deseadas

### User Story Dependencies

- **US1 (P1)**: tras Foundational; sin dependencias de otras historias (introduce `resolveCanApproveOnCreate/Update` y la auditoría)
- **US2 (P1)**: tras US1 (extiende los métodos `resolve...` y reutiliza el mapeo del DTO)
- **US3 (P2)**: tras US2 (misma función `resolveCanApproveOnUpdate`; las ramas deben ordenarse juntas)
- **US4 (P2)**: independiente de US2/US3; solo requiere Foundational (T005) — puede hacerse en paralelo con ellas
- **US5 (P2)**: independiente de US1–US4 en código (repositorio + método nuevo de `UserService`); puede hacerse en paralelo salvo por los archivos compartidos `UserServiceImpl.java`/`UserServiceTest.java` (coordinar el merge)

### Within Each User Story

- Pruebas primero y deben fallar → implementación → ejecución de las pruebas
- Modelo/DTO (Foundational) → servicio → controlador/mapper

### Parallel Opportunities

- Foundational: T002–T007 son [P] (archivos distintos)
- Dentro de cada historia, las tareas de prueba [P] tocan archivos distintos (`UserServiceTest`, `UserControllerWebMvcTest`, `UserMapperTest`, `UpdateUserRequestDtoTest`)
- US4 (mapper/controlador) y US5 (repositorio/interfaz) pueden avanzar en paralelo con US2/US3
- T034 y T035 en Polish

---

## Parallel Example: Foundational

```text
Task: "T002 Crear V3__add_can_approve_to_users.sql en src/main/resources/db/migration/"
Task: "T003 Añadir canApprove a src/main/java/co/edu/docurural/user/entity/User.java"
Task: "T005 Añadir canApprove a los tres response DTO en src/main/java/co/edu/docurural/user/dto/"
Task: "T006 Añadir la clave user.can-approve.reader-not-allowed en messages.properties"
```

## Parallel Example: User Story 1 (pruebas)

```text
Task: "T009 Pruebas de servicio en UserServiceTest.java"
Task: "T010 Pruebas de controlador en UserControllerWebMvcTest.java"
Task: "T011 Prueba de DTO en UpdateUserRequestDtoTest.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2, ambas P1)

1. Phase 1 → Phase 2 (fundacional)
2. Phase 3: US1 → validar de forma independiente
3. Phase 4: US2 → validar: sin READER aprobador (SC-002)
4. **PARAR y VALIDAR** con `./mvnw clean verify`

### Incremental Delivery

1. Fundacional listo
2. US1 → US2 (MVP: asignar el permiso con la regla de integridad)
3. US3 → retiro automático
4. US4 → visibilidad en respuestas (puede ir en paralelo)
5. US5 → `isActiveApprover` (requisito para las historias posteriores de aprobación)
6. Polish → CHANGELOG, `verify`, quickstart

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- No modificar V1/V2 ni `GlobalExceptionHandler`; `ActivityLogService` no cambia
- Un módulo distinto de `user` nunca debe usar `UserRepository`: la verificación de aprobador se consume solo vía `UserService.isActiveApprover`
- Commit tras cada tarea o grupo lógico
