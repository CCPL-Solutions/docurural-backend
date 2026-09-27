---

description: "Lista de tareas para HU-31, configuración de aprobación por categoría"
---

# Tasks: Configuración de aprobación por categoría (HU-31)

**Input**: Documentos de diseño en `/specs/002-category-approval-config/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/categories-api.md, quickstart.md

**Tests**: INCLUIDAS. No son opcionales: la constitución (Principios IV y V) exige pruebas por capa, fixtures de `TestFixtures` y cobertura JaCoCo ≥80% líneas / ≥65% ramas.

**Organization**: Tareas agrupadas por historia de usuario. El código vive en `category` (`src/main/java/co/edu/docurural/category/`), con un método nuevo en `user`. Clases de producción nuevas: `CategoryQueryService`, `CategoryQueryServiceImpl` y el `record` `ApprovalNotices`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede ejecutar en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1..US5)
- Nombres de prueba: `<accion>_<contexto>_<resultado>`; objetos de dominio solo con builders de `TestFixtures`

## Path Conventions

- Producción: `src/main/java/co/edu/docurural/category/{entity,dto,mapper,service,controller}/` y `src/main/java/co/edu/docurural/user/{repository,service}/`
- Recursos: `src/main/resources/`
- Pruebas: `src/test/java/co/edu/docurural/category/{service,controller,mapper}/`, `src/test/java/co/edu/docurural/user/service/`, fixtures en `src/test/java/co/edu/docurural/support/TestFixtures.java`

---

## Phase 1: Setup

**Purpose**: verificar el punto de partida

- [X] T001 Confirmar línea base verde ejecutando `./mvnw clean verify` en la raíz del repo, y confirmar que en `src/main/java/co/edu/docurural/category/controller/CategoryController.java` `create` (`POST`) y `update` (`PUT /{id}`) llevan `@PreAuthorize("hasRole('ADMIN')")` y `list`/`findById` (`GET`) llevan `@PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")` (research R7; si no, anotarlo como hallazgo aparte, no corregirlo aquí)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: columna, entidad, DTO, mensajes, firmas del mapper y fixtures que TODAS las historias necesitan

**⚠️ CRITICAL**: ninguna historia puede empezar hasta terminar esta fase

- [X] T002 [P] Crear `src/main/resources/db/migration/V4__add_requires_approval_to_categories.sql` (NO tocar V1–V3) con un comentario de cabecera `-- HU-31: indica qué categorías requieren aprobación; las existentes quedan en false.` y `ALTER TABLE categories ADD COLUMN IF NOT EXISTS requires_approval BOOLEAN NOT NULL DEFAULT FALSE;` (sin `CHECK` ni índice, research R1)
- [X] T003 [P] Añadir a `src/main/java/co/edu/docurural/category/entity/Category.java` el campo `private boolean requiresApproval;` con `@Builder.Default` = `false` y `@Column(name = "requires_approval", nullable = false)` (columna `requires_approval` `BOOLEAN NOT NULL DEFAULT FALSE`)
- [X] T004 [P] Añadir el último componente `@Nullable Boolean requiresApproval` (sin anotación de `jakarta.validation.constraints`; null = omitido, FR-002/FR-003) con `@Schema(description = "Indica si los documentos de la categoría requieren aprobación (omitido = false al crear; conserva el valor al editar)", example = "true", nullable = true)` en `src/main/java/co/edu/docurural/category/dto/CreateCategoryRequestDto.java` y `src/main/java/co/edu/docurural/category/dto/UpdateCategoryRequestDto.java`; actualizar todos los puntos que construyen estos records (pruebas existentes, `TestFixtures`) para que compilen pasando `null`. Ver Complexity Tracking del plan (declarar en el PR)
- [X] T005 [P] Añadir componentes a los response DTO en `src/main/java/co/edu/docurural/category/dto/`: `boolean requiresApproval` en `CategoryDetailResponseDto.java` (al final); `boolean requiresApproval` y `String approverWarning` (`@Schema(nullable = true)`) en `CreateCategoryResponseDto.java`, antes de `message`; `boolean requiresApproval`, `String approvalScopeNotice` y `String approverWarning` (ambos `@Schema(nullable = true)`) en `UpdateCategoryResponseDto.java`, antes de `message`. NO tocar `UpdateCategoryStatusResponseDto` (contrato: sin cambios). Usar como ejemplos de `@Schema` los textos de `contracts/categories-api.md`
- [X] T006 [P] Crear `src/main/java/co/edu/docurural/category/dto/ApprovalNotices.java`: `record ApprovalNotices(String scopeNotice, String approverWarning)` con Javadoc breve ("aviso de alcance y advertencia de pocos aprobadores que acompañan la respuesta de edición; cada uno es null cuando no aplica") y un factory `static ApprovalNotices none()` que devuelve ambos en `null` (research R5)
- [X] T007 [P] Añadir en `src/main/resources/messages.properties`, junto a las claves `category.*`: `category.requires-approval.scope-notice=Este cambio solo afecta a los documentos que se carguen desde ahora. Los documentos existentes conservan su estado actual` y `category.requires-approval.few-approvers=Hay menos de dos usuarios con permiso de aprobar. Los documentos que cargue un aprobador no podrán ser aprobados por él mismo`
- [X] T008 Cambiar firmas en `src/main/java/co/edu/docurural/category/mapper/CategoryMapper.java`: `toCreateResponse(Category category, String message, String approverWarning)` y `toUpdateResponse(Category category, String message, ApprovalNotices notices)` con `@Mapping(target = "approvalScopeNotice", source = "notices.scopeNotice")` y `@Mapping(target = "approverWarning", source = "notices.approverWarning")`; `requiresApproval` se mapea por nombre en todos (detalle, creación, edición). Ajustar `CategoryServiceImpl` para compilar pasando `null` y `ApprovalNotices.none()` provisionalmente (depende de T003, T005, T006)
- [X] T009 [P] En `src/test/java/co/edu/docurural/support/TestFixtures.java` añadir `categoryRequiringApproval(Long id, String name)` (ACTIVE, `requiresApproval(true)`), y sobrecargas `createCategoryRequest(String name, String description, Boolean requiresApproval)` y `updateCategoryRequest(String name, String description, Boolean requiresApproval)` (sensibilidad `INTERNAL`); las sobrecargas existentes pasan `null` en el campo nuevo
- [X] T010 Verificar que `./mvnw clean verify` compila y las pruebas existentes siguen verdes con T002–T009; corregir en `CategoryMapperTest`/`CategoryServiceTest`/`CategoryControllerWebMvcTest` solo las llamadas que rompan por las firmas nuevas

**Checkpoint**: modelo, DTO, mapper y fixtures listos; las historias pueden empezar

---

## Phase 3: User Story 1 - Marcar una categoría como "Requiere aprobación" (Priority: P1) 🎯 MVP

**Goal**: el ADMIN crea/edita categorías enviando `requiresApproval`; omitido al crear → `false`; omitido al editar → conserva; solo ADMIN escribe.

**Independent Test**: crear sin el campo → `false`; crear con `true` → `true`; editar `false → true` → queda `true`; editar sin el campo → conserva.

### Tests for User Story 1

> Escribirlas primero y comprobar que FALLAN antes de implementar

- [X] T011 [P] [US1] En `src/test/java/co/edu/docurural/category/service/CategoryServiceTest.java` añadir: `create_setsRequiresApprovalFalse_whenFieldOmitted`, `create_persistsRequiresApprovalTrue_whenFlagTrue`, `update_setsRequiresApprovalTrue_whenFlagSent`, `update_keepsRequiresApproval_whenFieldOmitted` (categoría de `categoryRequiringApproval` + request con `null` → sigue `true`), `update_setsRequiresApprovalFalse_whenFlagFalse`. Capturar la entidad guardada con `ArgumentCaptor<Category>`
- [X] T012 [P] [US1] En `src/test/java/co/edu/docurural/category/controller/CategoryControllerWebMvcTest.java` añadir `create_returns201WithRequiresApproval_whenFlagSent` y `update_returns200WithRequiresApproval_whenFlagSent` (JSON de request con `"requiresApproval": true`; verificar `$.requiresApproval`), y `createAndUpdate_requireAdminRole` que compruebe por reflexión que los métodos `create` y `update` del controlador llevan `@PreAuthorize` con valor `hasRole('ADMIN')` (la seguridad está excluida del `@WebMvcTest`, research R7)

### Implementation for User Story 1

- [X] T013 [US1] En `src/main/java/co/edu/docurural/category/service/CategoryServiceImpl.java`, en `create`, fijar `.requiresApproval(Boolean.TRUE.equals(request.requiresApproval()))` en el builder (null → `false`, FR-002)
- [X] T014 [US1] En `CategoryServiceImpl.update`/`applyUpdates` aplicar el valor solo si `request.requiresApproval() != null` y difiere del actual (null → conserva, FR-003); guardar el valor anterior en una variable local `previousRequiresApproval` para las historias US2/US3; mantener métodos ≤3 parámetros (depende de T013, mismo archivo)
- [X] T015 [US1] Ejecutar `./mvnw test -Dtest=CategoryServiceTest,CategoryControllerWebMvcTest` y confirmar que T011–T012 pasan

**Checkpoint**: US1 funcional y comprobable por sí sola (MVP)

---

## Phase 4: User Story 2 - Aviso de alcance del cambio y trazabilidad (Priority: P1)

**Goal**: cuando el valor cambia al editar, la respuesta trae `approvalScopeNotice` y `EDIT_CATEGORY` registra `requires_approval: a → b`; la creación registra el valor inicial; ningún documento cambia.

**Independent Test**: editar `false → true` → respuesta con el aviso y bitácora con `requires_approval: false → true`; editar sin cambio → sin aviso ni línea; `CREATE_CATEGORY` con `(requires_approval: <valor>)`; `DocumentCommandService` sin interacciones.

### Tests for User Story 2

- [X] T016 [P] [US2] En `src/test/java/co/edu/docurural/category/service/CategoryServiceTest.java` añadir: `update_returnsScopeNotice_whenRequiresApprovalChanges` (ambos sentidos), `update_omitsScopeNotice_whenRequiresApprovalUnchanged` (mismo valor y `null`), `update_logsRequiresApprovalChange_whenValueChanges` (capturar el `detail` de `activityLogService.record(eq(ActivityAction.EDIT_CATEGORY), ...)` y comprobar que contiene `requires_approval: false → true`), `update_doesNotLogRequiresApproval_whenValueUnchanged`, `update_doesNotTouchDocuments_whenOnlyRequiresApprovalChanges` (`verifyNoInteractions(documentCommandService)`), `create_logsInitialRequiresApproval_always` (detalle exacto `Categoria creada: <nombre> (requires_approval: true)` y otro caso con `false`)
- [X] T017 [P] [US2] En `src/test/java/co/edu/docurural/category/mapper/CategoryMapperTest.java` añadir `toUpdateResponse_mapsNotices_whenPresent` y `toUpdateResponse_leavesNoticesNull_whenNone` (`ApprovalNotices.none()`)

### Implementation for User Story 2

- [X] T018 [US2] En `CategoryServiceImpl.update`, cuando el valor cambió, añadir a `modifiedFields` la entrada `"requires_approval: " + previous + " → " + nuevo` (mismo estilo que `defaultSensitivityLevel: A → B`) y construir `ApprovalNotices` con `scopeNotice = messageResolver.get("category.requires-approval.scope-notice")`; si no cambió, `ApprovalNotices.none()`. No invocar `DocumentCommandService` por este cambio (FR-008). Extraer el cálculo a un método privado, sin flag booleano de bifurcación (Principio III)
- [X] T019 [US2] En `CategoryServiceImpl.create`, cambiar el detalle de `CREATE_CATEGORY` a `"Categoria creada: " + saved.getName() + " (requires_approval: " + saved.isRequiresApproval() + ")"` (FR-011, clarificación 3); actualizar el `log.info` de create/update para incluir `requiresApproval` (depende de T018, mismo archivo)
- [X] T020 [US2] Ejecutar `./mvnw test -Dtest=CategoryServiceTest,CategoryMapperTest` y confirmar que T016–T017 pasan

**Checkpoint**: US1 + US2 cubren las dos historias P1

---

## Phase 5: User Story 3 - Advertencia por pocos aprobadores (Priority: P2)

**Goal**: al pasar el indicador a `true` (crear con `true` o editar `false → true`) con menos de 2 aprobadores activos, la respuesta trae `approverWarning`; el guardado se completa.

**Independent Test**: con 0 o 1 aprobadores activos, activar → 200/201 con advertencia; con 2 → sin advertencia; desactivar o sin cambio → no se consulta el conteo.

### Tests for User Story 3

- [X] T021 [P] [US3] En `src/test/java/co/edu/docurural/user/service/UserServiceTest.java` añadir `countActiveApprovers_delegatesToRepository_withActiveStatusAndExcludingReader` (verifica `countByCanApproveTrueAndStatusAndRoleNot(UserStatus.ACTIVE, UserRole.READER)` y devuelve su valor)
- [X] T022 [P] [US3] En `src/test/java/co/edu/docurural/category/service/CategoryServiceTest.java` añadir `@Mock UserService userService` y: `create_returnsApproverWarning_whenFlagTrueAndFewerThanTwoApprovers` (parametrizar 0 y 1), `create_omitsApproverWarning_whenTwoApprovers`, `create_skipsApproverCount_whenFlagFalse` (`verifyNoInteractions(userService)`), `update_returnsApproverWarning_whenActivatedAndOneApprover`, `update_omitsApproverWarning_whenDeactivated` (sin llamada a `countActiveApprovers`), `update_skipsApproverCount_whenAlreadyActive`

### Implementation for User Story 3

- [X] T023 [P] [US3] Añadir en `src/main/java/co/edu/docurural/user/repository/UserRepository.java` la consulta derivada `long countByCanApproveTrueAndStatusAndRoleNot(UserStatus status, UserRole role);`
- [X] T024 [US3] Añadir `long countActiveApprovers();` con Javadoc (definición: `can_approve`, `ACTIVE`, rol ≠ READER; misma que `isActiveApprover`) en `src/main/java/co/edu/docurural/user/service/UserService.java` e implementarlo en `src/main/java/co/edu/docurural/user/service/UserServiceImpl.java` con `@Transactional(readOnly = true)` (depende de T023)
- [X] T025 [US3] En `CategoryServiceImpl` inyectar `UserService` (declarado como interfaz, campo `final`), añadir la constante `private static final long MIN_ACTIVE_APPROVERS = 2;` y un método privado que devuelve el texto `category.requires-approval.few-approvers` solo si hay menos de `MIN_ACTIVE_APPROVERS`, invocado solo cuando el indicador pasa a `true`: en `create` si se crea con `true` (pasar a `toCreateResponse` como `approverWarning`) y en `update` si cambió `false → true` (en `ApprovalNotices`). Si no aplica, `null` sin consultar el conteo (depende de T018, T019, T024)
- [X] T026 [US3] Ejecutar `./mvnw test -Dtest=CategoryServiceTest,UserServiceTest` y confirmar que T021–T022 pasan

**Checkpoint**: la advertencia funciona sin bloquear el guardado

---

## Phase 6: User Story 4 - Consultar el indicador en el listado y en el detalle (Priority: P2)

**Goal**: el listado y el detalle exponen `requiresApproval` para ADMIN y EDITOR; se entrega `CategoryQueryService.requiresApproval(categoryId)` para HU-33 (FR-014).

**Independent Test**: listado con categorías con y sin el indicador → cada una expone su valor; `requiresApproval(id)` devuelve el valor guardado y 404 si no existe.

### Tests for User Story 4

- [X] T027 [P] [US4] En `src/test/java/co/edu/docurural/category/mapper/CategoryMapperTest.java` añadir `toDetailResponse_exposesRequiresApproval`, `toListResponse_exposesRequiresApproval_forEachCategory` (una con `true`, otra con `false`) y `toCreateResponse_mapsRequiresApprovalAndWarning`
- [X] T028 [P] [US4] En `src/test/java/co/edu/docurural/category/controller/CategoryControllerWebMvcTest.java` añadir `list_returnsRequiresApproval_forEachCategory` y `findById_returnsRequiresApproval` (verificar `$.categories[0].requiresApproval` y `$.requiresApproval`)
- [X] T029 [P] [US4] Crear `src/test/java/co/edu/docurural/category/service/CategoryQueryServiceTest.java` (`@ExtendWith(MockitoExtension.class)`, `@Mock CategoryRepository`, `@Mock MessageResolver`, `@InjectMocks CategoryQueryServiceImpl`) con `requiresApproval_returnsTrue_whenCategoryRequiresApproval`, `requiresApproval_returnsFalse_whenCategoryDoesNotRequireApproval` y `requiresApproval_throwsNotFound_whenCategoryMissing` (`ResourceNotFoundException`, clave `category.not-found`)

### Implementation for User Story 4

- [X] T030 [P] [US4] Crear `src/main/java/co/edu/docurural/category/service/CategoryQueryService.java` con `boolean requiresApproval(Long categoryId);` y Javadoc: interfaz de solo lectura del módulo `category` sin dependencias del módulo `document` (resuelve el ciclo previsto en D-4); los consumidores deben invocarla en cada carga, sin cachear
- [X] T031 [US4] Crear `src/main/java/co/edu/docurural/category/service/CategoryQueryServiceImpl.java` (`@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`), colaboradores solo `CategoryRepository` y `MessageResolver`: `categoryRepository.findById(id).map(Category::isRequiresApproval).orElseThrow(() -> new ResourceNotFoundException(messageResolver.get("category.not-found", id)))` (depende de T030)
- [X] T032 [US4] Ejecutar `./mvnw test -Dtest=CategoryMapperTest,CategoryControllerWebMvcTest,CategoryQueryServiceTest` y confirmar que T027–T029 pasan

**Checkpoint**: lectura para ADMIN/EDITOR y contrato para HU-33 listos

---

## Phase 7: User Story 5 - Estado inicial de las categorías existentes (Priority: P3)

**Goal**: tras la migración, las 8 categorías predefinidas quedan con `requiresApproval = false`.

**Independent Test**: aplicar V4 sobre una base con las 8 categorías y listar → todas en `false`.

- [X] T033 [US5] Arrancar con `./mvnw spring-boot:run` (perfil local) sobre una base con V1–V3 aplicadas y comprobar que Flyway aplica `V4__add_requires_approval_to_categories.sql` sin errores y que `SELECT name, requires_approval FROM categories;` devuelve `false` en las 8 categorías sembradas (quickstart §2). Si no hay base local disponible, dejarlo anotado como validación pendiente en la descripción del PR

**Checkpoint**: despliegue sin cambio de comportamiento hasta que el ADMIN active categorías

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T034 [P] Añadir en `CHANGELOG.md`, bajo `## [Unreleased]` → `### Added`, una viñeta: "Añadida la opción **Requiere aprobación** en las categorías documentales (HU-31): se configura al crear o editar, se muestra en el listado y el detalle, avisa del alcance del cambio y de si hay menos de dos aprobadores, y queda registrada en la bitácora."
- [X] T035 [P] Revisar el diff contra la constitución: sin imports de `UserRepository` nuevos en `category`, `CategoryQueryServiceImpl` sin dependencias de `document`, métodos ≤3 parámetros, sin strings de mensaje hardcodeados, un `log.info` por operación exitosa
- [X] T036 Ejecutar `./mvnw clean verify` y confirmar verde con los umbrales JaCoCo (≥80% líneas, ≥65% ramas)
- [X] T037 Recorrer los pasos de `specs/002-category-approval-config/quickstart.md` §3 que sean posibles en local y preparar la descripción del PR con: la desviación `@Nullable Boolean` sin restricción (Complexity Tracking), el cambio de `CategoryService` a `CategoryQueryService` para FR-014 y la sugerencia de registrar en D-4 el uso de `UserRepository.getReferenceById` en `CategoryServiceImpl.create`
- [X] T038 Cambiar `**Status**: Draft` a `**Status**: Implemented` en `specs/002-category-approval-config/spec.md` una vez mergeado

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA todas las historias
- **US1 (Phase 3)**: depende de Foundational
- **US2 (Phase 4)**: depende de US1 (usa `previousRequiresApproval` de T014 y edita el mismo archivo)
- **US3 (Phase 5)**: depende de US2 (T025 edita las mismas ramas de `CategoryServiceImpl`); T021, T023 y T024 pueden adelantarse en paralelo porque tocan `user`
- **US4 (Phase 6)**: depende solo de Foundational; puede ir en paralelo con US1–US3 (archivos distintos salvo `CategoryMapperTest`/`CategoryControllerWebMvcTest`, coordinar con T012/T017)
- **US5 (Phase 7)**: depende solo de T002
- **Polish (Phase 8)**: depende de todas las historias

### Within Each User Story

- Pruebas primero y deben fallar → implementación → ejecución de las pruebas
- DTO/entidad (Foundational) → servicio → mapper/controlador

### Parallel Opportunities

- Foundational: T002–T007 y T009 son [P] (archivos distintos); T008 espera a T003, T005 y T006
- US3: T021 + T023 → T024 en `user`, en paralelo con US1/US2 en `category`
- US4: T027–T030 en paralelo; toda la fase puede avanzar junto con US1–US3
- Polish: T034 y T035

---

## Parallel Example: Foundational

```text
Task: "T002 Crear V4__add_requires_approval_to_categories.sql"
Task: "T003 Añadir requiresApproval a Category.java"
Task: "T004 Añadir requiresApproval a los request DTO"
Task: "T005 Añadir campos a los response DTO"
Task: "T006 Crear ApprovalNotices.java"
Task: "T007 Añadir las dos claves en messages.properties"
Task: "T009 Fixtures en TestFixtures.java"
```

## Parallel Example: User Story 4

```text
Task: "T027 Pruebas de mapper en CategoryMapperTest.java"
Task: "T029 Crear CategoryQueryServiceTest.java"
Task: "T030 Crear CategoryQueryService.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2, ambas P1)

1. Phase 1 → Phase 2 (fundacional)
2. Phase 3: US1 → validar de forma independiente
3. Phase 4: US2 → validar aviso, bitácora y que no se tocan documentos
4. **PARAR y VALIDAR** con `./mvnw clean verify`

### Incremental Delivery

1. Fundacional listo
2. US1 → US2 (MVP: configurar el indicador con trazabilidad)
3. US3 → advertencia de pocos aprobadores
4. US4 → lectura para EDITOR y `CategoryQueryService` (requisito de HU-33; puede ir en paralelo)
5. US5 → validación de la migración
6. Polish → CHANGELOG, `verify`, PR

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- No modificar V1–V3, `GlobalExceptionHandler`, `ActivityLogService` ni `SecurityConfig`
- `category` nunca debe usar `UserRepository` para esta historia: el conteo se pide a `UserService.countActiveApprovers()`
- `CategoryQueryServiceImpl` no debe depender de nada del módulo `document`
- Commit tras cada tarea o grupo lógico
