---

description: "Lista de tareas para HU-33, carga de documento en estado Borrador"
---

# Tasks: Carga de documento en estado Borrador (HU-33)

**Input**: Documentos de diseño en `/specs/003-draft-document-upload/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/documents-api.md, quickstart.md

**Tests**: INCLUIDAS. No son opcionales: la constitución (Principios IV y V) exige pruebas por capa, fixtures de `TestFixtures` y cobertura JaCoCo ≥80% líneas / ≥65% ramas.

**Organization**: Tareas agrupadas por historia de usuario. El código vive en `document` (`src/main/java/co/edu/docurural/document/`) y consume `CategoryQueryService` de `category` (HU-31). Clase de producción nueva: el enum `DocumentWorkflowStatus`. Sin endpoints nuevos.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede ejecutar en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia a la que pertenece (US1..US5)
- Nombres de prueba: `<accion>_<contexto>_<resultado>`; objetos de dominio solo con builders de `TestFixtures`

## Path Conventions

- Producción: `src/main/java/co/edu/docurural/document/{enums,entity,dto,mapper,service}/`
- Recursos: `src/main/resources/`
- Pruebas: `src/test/java/co/edu/docurural/document/{service,mapper,controller}/`, fixtures en `src/test/java/co/edu/docurural/support/TestFixtures.java`

---

## Phase 1: Setup

**Purpose**: verificar el punto de partida

- [ ] T001 Confirmar línea base verde ejecutando `./mvnw clean verify` en la raíz del repo, y confirmar que `src/main/java/co/edu/docurural/category/service/CategoryQueryService.java` expone `boolean requiresApproval(Long categoryId)` (HU-31) y que `CategoryQueryServiceImpl` es `@Transactional(readOnly = true)` sin dependencias de `document` (research R1)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: esquema, enum, entidad, DTO, mapper, mensaje y fixtures que TODAS las historias necesitan

**⚠️ CRITICAL**: ninguna historia puede empezar hasta terminar esta fase

- [ ] T002 [P] Crear `src/main/resources/db/migration/V5__add_workflow_status_to_documents.sql` (NO tocar V1–V4) con comentario de cabecera `-- HU-33: estado del flujo de aprobación de documentos; los activos existentes pasan a APPROVED (D-08).` y, en este orden (research R3, data-model.md): `ALTER TABLE documents ADD COLUMN IF NOT EXISTS workflow_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED';` · `ALTER TABLE documents ADD COLUMN IF NOT EXISTS cycle_number INT NOT NULL DEFAULT 0;` · `ALTER TABLE documents DROP CONSTRAINT IF EXISTS ck_documents_workflow_status;` + `ALTER TABLE documents ADD CONSTRAINT ck_documents_workflow_status CHECK (workflow_status IN ('NOT_REQUIRED', 'DRAFT', 'IN_REVIEW', 'APPROVED', 'ARCHIVED'));` · `ALTER TABLE documents DROP CONSTRAINT IF EXISTS ck_documents_cycle_number;` + `ALTER TABLE documents ADD CONSTRAINT ck_documents_cycle_number CHECK (cycle_number >= 0);` · `UPDATE documents SET workflow_status = 'APPROVED' WHERE status = 'ACTIVE' AND workflow_status = 'NOT_REQUIRED';` · `CREATE INDEX IF NOT EXISTS idx_documents_workflow_status ON documents (workflow_status);`
- [ ] T003 [P] Crear `src/main/java/co/edu/docurural/document/enums/DocumentWorkflowStatus.java`: `enum DocumentWorkflowStatus { NOT_REQUIRED, DRAFT, IN_REVIEW, APPROVED, ARCHIVED }` con Javadoc breve (estado del documento en el flujo de aprobación RF-07, independiente de `DocumentStatus`) y `public static DocumentWorkflowStatus initialFor(boolean requiresApproval)` que devuelve `DRAFT` si `true` y `NOT_REQUIRED` si `false` (research R1)
- [ ] T004 Añadir a `src/main/java/co/edu/docurural/document/entity/Document.java` (depende de T003): `@Enumerated(EnumType.STRING) @Column(name = "workflow_status", nullable = false, length = 20) private DocumentWorkflowStatus workflowStatus;` y `@Column(name = "cycle_number", nullable = false) private int cycleNumber;` (0 por defecto en Java); en `@PrePersist onCreate()` añadir `if (workflowStatus == null) { workflowStatus = DocumentWorkflowStatus.NOT_REQUIRED; }` siguiendo el patrón de `status`/`sensitivityLevel`
- [ ] T005 [P] Añadir el componente `String workflowStatus` con `@Schema(description = "Estado del documento en el flujo de aprobación", example = "DRAFT", allowableValues = {"NOT_REQUIRED", "DRAFT", "IN_REVIEW", "APPROVED", "ARCHIVED"})` en `src/main/java/co/edu/docurural/document/dto/UploadDocumentResponseDto.java` (antes de `message`), `src/main/java/co/edu/docurural/document/dto/DocumentDetailResponseDto.java` (al final, tras `sensitivityLevel`) y `src/main/java/co/edu/docurural/document/dto/UpdateDocumentMetadataResponseDto.java` (antes de `message`); en `src/main/java/co/edu/docurural/document/dto/BatchUploadItemResultDto.java` añadirlo tras `documentId` con `nullable = true` y descripción "Estado del flujo del documento creado. null si la carga falló". NO tocar `DocumentSummaryResponseDto` (listado/búsqueda, HU-42)
- [ ] T006 Añadir en `src/main/java/co/edu/docurural/document/mapper/DocumentMapper.java` (depende de T004, T005) `@Mapping(target = "workflowStatus", expression = "java(document.getWorkflowStatus() != null ? document.getWorkflowStatus().name() : null)")` a `toUploadResponse`, `toDetailResponse` y `toUpdateMetadataResponse` (mismo patrón que `sensitivityLevel`, research R6); ajustar las llamadas a `new BatchUploadItemResultDto(...)` en `src/main/java/co/edu/docurural/document/service/DocumentBatchServiceImpl.java` pasando `null` provisionalmente en el componente nuevo
- [ ] T007 [P] Añadir en `src/main/resources/messages.properties`, junto a `document.uploaded.success`: `document.uploaded.draft=Documento cargado como borrador` (research R4)
- [ ] T008 [P] En `src/test/java/co/edu/docurural/support/TestFixtures.java`: hacer que ambas sobrecargas de `documentActive(...)` fijen `.workflowStatus(DocumentWorkflowStatus.NOT_REQUIRED)` y añadir `documentDraft(Long id, Category category, User uploadedBy)` igual que `documentActive` pero con `.workflowStatus(DocumentWorkflowStatus.DRAFT)` (depende de T003, T004)
- [ ] T009 Ejecutar `./mvnw clean verify` con T002–T008 y corregir solo lo que no compile por los records ampliados: las construcciones `new UploadDocumentResponseDto(...)`, `new DocumentDetailResponseDto(...)`, `new UpdateDocumentMetadataResponseDto(...)` y `new BatchUploadItemResultDto(...)` en `src/test/java/co/edu/docurural/document/controller/DocumentControllerWebMvcTest.java` y `src/test/java/co/edu/docurural/document/controller/DocumentBatchControllerWebMvcTest.java`; además, en `src/test/java/co/edu/docurural/document/service/DocumentCommandServiceTest.java` añadir `@Mock CategoryQueryService categoryQueryService;` y, en el `@BeforeEach` existente, `lenient().when(categoryQueryService.requiresApproval(any())).thenReturn(false);` para que las historias US1–US4 puedan usarlo sin depender entre sí

**Checkpoint**: esquema, modelo, DTO, mapper y fixtures listos; las historias pueden empezar

---

## Phase 3: User Story 1 - Carga individual en una categoría con aprobación queda en Borrador (Priority: P1) 🎯 MVP

**Goal**: una carga individual en una categoría con "Requiere aprobación" crea el documento en `DRAFT`, responde `workflowStatus: DRAFT` con "Documento cargado como borrador" y registra `workflow_status: DRAFT` en la bitácora; el cliente no puede fijar el estado.

**Independent Test**: con `CategoryQueryService.requiresApproval` devolviendo `true`, `upload` persiste un `Document` con `workflowStatus = DRAFT`, `cycleNumber = 0` y hash calculado, la respuesta trae `DRAFT` y el mensaje de borrador, y `activityLogService.record(UPLOAD, …, "Archivo: acta.pdf; workflow_status: DRAFT")`.

### Tests for User Story 1 ⚠️

> Escribir primero y verificar que FALLAN antes de implementar

- [ ] T010 [US1] En `src/test/java/co/edu/docurural/document/service/DocumentCommandServiceTest.java` (con el mock `categoryQueryService` creado en T009) añadir las pruebas: `upload_createsDraft_whenCategoryRequiresApproval` (captura con `ArgumentCaptor<Document>` el `save` y verifica `workflowStatus == DRAFT` y `cycleNumber == 0`), `upload_returnsDraftMessageAndStatus_whenCategoryRequiresApproval` (respuesta con `workflowStatus = "DRAFT"` y mensaje resuelto de `document.uploaded.draft`), `upload_logsWorkflowStatusDraft_whenCategoryRequiresApproval` (`verify(activityLogService).record(eq(ActivityAction.UPLOAD), eq(AUDIT), eq(48L), eq("Archivo: acta.pdf; workflow_status: DRAFT"))`), `upload_calculatesHash_whenDocumentIsDraft` (verifica `documentHashService.calculateSha256(file)` y `fileHash` en el `Document` guardado) y `upload_readsRequiresApprovalOnEachUpload_whenCalledTwice` (dos cargas con `requiresApproval` `true` y luego `false` producen `DRAFT` y luego `NOT_REQUIRED`; `verify(categoryQueryService, times(2)).requiresApproval(CATEGORY_ID)`); usar `TestFixtures.categoryRequiringApproval(id, name)` y `uploadDocumentRequest(categoryId)`
- [ ] T011 [P] [US1] En `src/test/java/co/edu/docurural/document/controller/DocumentControllerWebMvcTest.java` añadir `upload_returns201WithDraftStatus_whenServiceReturnsDraft` (el servicio mockeado devuelve `UploadDocumentResponseDto` con `workflowStatus = "DRAFT"` y mensaje "Documento cargado como borrador"; verificar `jsonPath("$.workflowStatus").value("DRAFT")` y `$.message`) y `upload_ignoresWorkflowStatusField_whenClientSendsIt` (la parte `data` incluye `"workflowStatus":"APPROVED"`; responde 201 y el `UploadDocumentRequestDto` capturado no tiene ese componente, FR-005)
- [ ] T012 [P] [US1] En `src/test/java/co/edu/docurural/document/mapper/DocumentMapperTest.java` añadir `toUploadResponse_mapsWorkflowStatus_whenDraft` (con `TestFixtures.documentDraft(...)` → `"DRAFT"`) y `toUploadResponse_mapsNullWorkflowStatus_whenStatusIsNull`

### Implementation for User Story 1

- [ ] T013 [US1] En `src/main/java/co/edu/docurural/document/service/DocumentCommandServiceImpl.java` inyectar `private final CategoryQueryService categoryQueryService;` (interfaz de `co.edu.docurural.category.service`, nunca `CategoryRepository` para esto) y en `processSingleFile`, **después** de `validateFile(file)`, del cálculo del hash y de `fileValidationService.validate(file)` y **antes** de `storeWithRollback` (un archivo inválido no consulta la categoría ni se almacena): obtener `DocumentWorkflowStatus workflowStatus = DocumentWorkflowStatus.initialFor(categoryQueryService.requiresApproval(category.getId()))`. **NO cambiar las firmas de `buildDocument` ni de `processSingleFile`** (ya incumplen el límite de 3 parámetros del Principio III; no se agrava): asignar el estado sobre el objeto construido con `document.setWorkflowStatus(workflowStatus)` entre `buildDocument(...)` y `documentRepository.save(document)`. Registrar la bitácora con `activityDetailPrefix + saved.getOriginalFileName() + "; workflow_status: " + saved.getWorkflowStatus().name()` (research R5). El estado nunca se lee del request (FR-005)
- [ ] T014 [US1] En el mismo `DocumentCommandServiceImpl.java`: añadir `private static String uploadMessageKey(DocumentWorkflowStatus status)` que devuelve `"document.uploaded.draft"` para `DRAFT` y `"document.uploaded.success"` en otro caso; usarlo en `upload` al construir `documentMapper.toUploadResponse(saved, messageResolver.get(uploadMessageKey(saved.getWorkflowStatus())))`; añadir `workflowStatus={}` al `log.info` de "Documento cargado" (depende de T013)
- [ ] T015 [US1] Ejecutar `./mvnw test -Dtest=DocumentCommandServiceTest,DocumentControllerWebMvcTest,DocumentMapperTest` y dejar T010–T012 en verde; ajustar la verificación existente de `upload_persistsDocumentAndLogsActivity_whenAllValid` solo si su `anyString()` deja de ser suficiente

**Checkpoint**: la carga individual en categoría con aprobación queda en Borrador de punta a punta

---

## Phase 4: User Story 2 - Carga individual en categoría sin aprobación conserva el MVP (Priority: P1)

**Goal**: en categorías sin aprobación el documento queda `NOT_REQUIRED`, el mensaje sigue siendo "Documento cargado exitosamente" y la bitácora registra `workflow_status: NOT_REQUIRED`; ninguna validación existente cambia.

**Independent Test**: con `requiresApproval` en `false`, `upload` persiste `NOT_REQUIRED`, responde con `document.uploaded.success` y registra `"Archivo: acta.pdf; workflow_status: NOT_REQUIRED"`; las pruebas existentes de validación siguen verdes sin cambios.

### Tests for User Story 2 ⚠️

- [ ] T016 [US2] En `src/test/java/co/edu/docurural/document/service/DocumentCommandServiceTest.java` añadir `upload_createsNotRequired_whenCategoryDoesNotRequireApproval` (captura `Document` guardado → `NOT_REQUIRED`, `cycleNumber == 0`), `upload_keepsSuccessMessage_whenCategoryDoesNotRequireApproval` (mensaje de `document.uploaded.success`, `workflowStatus = "NOT_REQUIRED"`) y `upload_logsWorkflowStatusNotRequired_whenCategoryDoesNotRequireApproval` (detalle exacto `"Archivo: acta.pdf; workflow_status: NOT_REQUIRED"`); usar `TestFixtures.categoryActive(id, name)`
- [ ] T017 [US2] En el mismo archivo, añadir `upload_doesNotQueryApproval_whenCategoryIsInactive` (categoría inactiva → `ResourceNotFoundException` y `verify(categoryQueryService, never()).requiresApproval(any())`) y `upload_doesNotQueryApproval_whenFileValidationFails` (si `fileValidationService.validate` lanza, no se guarda documento ni se registra UPLOAD); confirmar que las pruebas existentes de sensibilidad, rol, tamaño y formato siguen verdes sin modificarlas (FR-009)

### Implementation for User Story 2

- [ ] T018 [US2] Verificar (sin código nuevo esperado) que en `src/main/java/co/edu/docurural/document/service/DocumentCommandServiceImpl.java` la consulta `requiresApproval` quedó en la posición fijada por T013 y ejecutar `./mvnw test -Dtest=DocumentCommandServiceTest` hasta dejar T016–T017 en verde (depende de T013–T014)

**Checkpoint**: US1 y US2 cubren ambas ramas de la carga individual

---

## Phase 5: User Story 3 - Carga múltiple asigna el estado por documento (Priority: P2)

**Goal**: cada archivo exitoso del lote queda en el estado que corresponde a su categoría, su resultado expone `workflowStatus` (null en los fallidos) y cada UPLOAD registra `Carga múltiple — Archivo: …; workflow_status: X`.

**Independent Test**: lote de 3 archivos en categoría con aprobación donde 1 falla → 2 resultados `DRAFT` con `documentId`, 1 fallido con `workflowStatus = null`; los totales no cambian.

### Tests for User Story 3 ⚠️

- [ ] T019 [P] [US3] En `src/test/java/co/edu/docurural/document/service/DocumentCommandServiceTest.java` añadir `uploadSingleForBatch_createsDraftAndLogsBatchDetail_whenCategoryRequiresApproval` (guarda `DRAFT`; detalle exacto `"Carga múltiple — Archivo: acta.pdf; workflow_status: DRAFT"`) y `uploadSingleForBatch_createsNotRequired_whenCategoryDoesNotRequireApproval`
- [ ] T020 [P] [US3] En `src/test/java/co/edu/docurural/document/service/DocumentBatchServiceTest.java` añadir `uploadBatch_includesWorkflowStatusPerItem_whenAllFilesValid` (el `DocumentCommandService` mockeado devuelve `TestFixtures.documentDraft(...)`; cada resultado trae `workflowStatus = "DRAFT"`) y `uploadBatch_returnsNullWorkflowStatus_whenFileFails` (un `BusinessRuleException` → resultado fallido con `workflowStatus` null, los demás `DRAFT`, `totalSuccessful`/`totalFailed` correctos)
- [ ] T021 [P] [US3] En `src/test/java/co/edu/docurural/document/controller/DocumentBatchControllerWebMvcTest.java` añadir `uploadBatch_returns200WithWorkflowStatusPerItem_whenServiceReturnsResults` verificando `jsonPath("$.results[0].workflowStatus").value("DRAFT")` y `jsonPath("$.results[1].workflowStatus").doesNotExist()` o `isEmpty()` según cómo serialice `null` el proyecto

### Implementation for User Story 3

- [ ] T022 [US3] En `src/main/java/co/edu/docurural/document/service/DocumentBatchServiceImpl.java`, en `processOneFile`, construir el resultado exitoso con `new BatchUploadItemResultDto(fileName, true, saved.getId(), saved.getWorkflowStatus().name(), null)` y los fallidos con `workflowStatus` `null` (sustituye el `null` provisional de T006); añadir `workflowStatus={}` al `log.info` de "Documento cargado (lote)" en `DocumentCommandServiceImpl.uploadSingleForBatch` (el estado y el detalle ya los fija `processSingleFile` desde T013). Ejecutar `./mvnw test -Dtest=DocumentBatchServiceTest,DocumentCommandServiceTest,DocumentBatchControllerWebMvcTest`

**Checkpoint**: la carga múltiple no es un atajo para saltarse el flujo

---

## Phase 6: User Story 4 - El estado no cambia al editar la categoría del documento (Priority: P2)

**Goal**: la edición de metadatos (incluido el cambio de categoría) no recalcula el estado; la respuesta de la edición y la ficha exponen `workflowStatus` vigente (FR-007a, FR-010, FR-011).

**Independent Test**: editar un `documentDraft` moviéndolo a una categoría sin aprobación → respuesta con `workflowStatus = "DRAFT"` y `categoryQueryService` sin invocaciones; `findDetailById` de ese documento → `"DRAFT"`.

### Tests for User Story 4 ⚠️

- [ ] T023 [P] [US4] En `src/test/java/co/edu/docurural/document/service/DocumentCommandServiceTest.java` añadir `updateMetadata_keepsDraft_whenCategoryChangesToOneWithoutApproval` (documento `documentDraft`, request a `categoryActive` → `Document` guardado sigue `DRAFT`, respuesta `workflowStatus = "DRAFT"`, `verify(categoryQueryService, never()).requiresApproval(any())`), `updateMetadata_keepsNotRequired_whenCategoryChangesToOneRequiringApproval` (documento `documentActive` → sigue `NOT_REQUIRED`) y `deleteLogical_keepsWorkflowStatus_whenDocumentIsDraft` (tras `markAsDeleted` el estado sigue `DRAFT`)
- [ ] T024 [P] [US4] En `src/test/java/co/edu/docurural/document/service/DocumentQueryServiceTest.java` añadir `findDetailById_returnsWorkflowStatus_whenDocumentIsDraft` (detalle con `workflowStatus = "DRAFT"`)
- [ ] T025 [P] [US4] En `src/test/java/co/edu/docurural/document/mapper/DocumentMapperTest.java` añadir `toDetailResponse_mapsWorkflowStatus_whenDraft` y `toUpdateMetadataResponse_mapsWorkflowStatus_whenNotRequired`
- [ ] T026 [P] [US4] En `src/test/java/co/edu/docurural/document/controller/DocumentControllerWebMvcTest.java` añadir `findById_returnsWorkflowStatus_whenDocumentExists` y `updateMetadata_returnsWorkflowStatus_whenUpdated` verificando `jsonPath("$.workflowStatus")`

### Implementation for User Story 4

- [ ] T027 [US4] Confirmar que `applyMetadataUpdates` y `deleteLogical` en `src/main/java/co/edu/docurural/document/service/DocumentCommandServiceImpl.java` no tocan `workflowStatus` ni consultan `CategoryQueryService` (no se espera código nuevo: la garantía la dan T023–T026, research R7) y ejecutar `./mvnw test -Dtest=DocumentCommandServiceTest,DocumentQueryServiceTest,DocumentMapperTest,DocumentControllerWebMvcTest`

**Checkpoint**: el estado del flujo queda fijo tras la carga y visible en ficha y edición

---

## Phase 7: User Story 5 - Estado del flujo de los documentos existentes al desplegar (Priority: P3)

**Goal**: al aplicar V5, los activos existentes quedan `APPROVED`, los eliminados `NOT_REQUIRED`, todos con `cycle_number = 0`, sin aprobador (D-08).

**Independent Test**: arrancar en local sobre una base con documentos activos y eliminados y ejecutar la consulta de `quickstart.md` §2.

- [ ] T028 [US5] Validar `src/main/resources/db/migration/V5__add_workflow_status_to_documents.sql` (T002) arrancando con `./mvnw spring-boot:run` sobre una base local con documentos `ACTIVE` y `DELETED` previos y ejecutar `SELECT status, workflow_status, cycle_number, COUNT(*) FROM documents GROUP BY status, workflow_status, cycle_number;` → `ACTIVE/APPROVED/0` y `DELETED/NOT_REQUIRED/0`; comprobar que existen `ck_documents_workflow_status`, `ck_documents_cycle_number` e `idx_documents_workflow_status`, y que Flyway no reporta errores. Si algo falla y V5 aún no se ha aplicado en ningún entorno compartido, corregir V5; si ya se aplicó, crear V6 correctiva (Principio VI). No hay pruebas de integración en el proyecto (constitución, SHOULD), por eso esta validación es manual

**Checkpoint**: todas las historias funcionan de forma independiente

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: documentación, puerta de calidad y validación final

- [ ] T029 [P] Añadir en `CHANGELOG.md`, bajo `## [Unreleased]` → `### Added`, una viñeta: "Añadido el **estado del flujo de aprobación** en la carga de documentos (HU-33): los documentos cargados en categorías que requieren aprobación quedan en Borrador (`DRAFT`) y los demás Sin flujo (`NOT_REQUIRED`), tanto en carga individual como múltiple; el estado se expone como `workflowStatus` en las respuestas de carga, detalle y edición, no se recalcula al editar y se registra en la bitácora. Los documentos activos existentes pasan a Aprobado (`APPROVED`) por migración. **Condición de despliegue:** no pasar a producción antes de HU-42 (visibilidad de borradores)."
- [ ] T030 [P] Actualizar `CLAUDE.md` en la tabla "Módulo `document` — reparto CQRS" si hace falta para mencionar que `DocumentCommandService` fija el estado inicial del flujo con `CategoryQueryService.requiresApproval`; revisar `README.md` y alinear cualquier descripción de la carga o del modelo de `documents` con `workflow_status`/`cycle_number`
- [ ] T031 Ejecutar `./mvnw clean verify` en la raíz y confirmar verde con umbrales JaCoCo (≥80% líneas, ≥65% ramas) sin nuevas exclusiones; si alguna rama nueva queda sin cubrir, añadir la prueba en la clase de test correspondiente
- [ ] T032 Recorrer los escenarios manuales 1–9 de `specs/003-draft-document-upload/quickstart.md` §3 contra la aplicación en local y anotar cualquier desviación
- [ ] T033 Revisar el diff contra la constitución (imports cruzados: `document` solo usa `CategoryQueryService` de `category` para el estado, sin nuevos repositorios ajenos; sin strings de mensaje hardcodeados; sin `null` devuelto por métodos públicos nuevos) y preparar para la descripción del PR la justificación de Complexity Tracking del plan (`DocumentCommandServiceImpl` pasa de 10 a 11 colaboradores), la nota sobre el incumplimiento previo del límite de 3 parámetros en `processSingleFile`/`buildDocument` (no agravado; proponer registrarlo como desviación o corregirlo aparte) y la condición de despliegue hasta HU-42

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA todas las historias. Orden interno: T003 → T004 → T006/T008; T002, T005, T007 en paralelo; T009 al final
- **US1 (Phase 3)**: depende de Foundational. Introduce la lógica de estado inicial en `processSingleFile` (T013), que reutilizan US2 y US3
- **US2 (Phase 4)**: depende de US1 (misma lógica, rama `NOT_REQUIRED`); sus pruebas son independientes de las de US1
- **US3 (Phase 5)**: depende de US1 (T013 fija el estado y el detalle para el lote); independiente de US2 y US4
- **US4 (Phase 6)**: depende solo de Foundational (mapper y DTO de T005–T006, mock `categoryQueryService` de T009); puede ir en paralelo con US1–US3
- **US5 (Phase 7)**: depende solo de T002; puede validarse en cualquier momento tras Foundational
- **Polish (Phase 8)**: depende de todas las historias

### Within Each User Story

- Pruebas primero, verificando que fallan
- Servicio después de DTO/mapper (ya en Foundational)
- Historia completa antes de pasar a la siguiente prioridad

### Parallel Opportunities

- Foundational: T002, T003, T005, T007 en paralelo; luego T004; luego T006 y T008 en paralelo
- US1: T011 y T012 en paralelo con T010 (archivos distintos)
- US3: T019, T020, T021 en paralelo (tres archivos distintos)
- US4: T023–T026 en paralelo (cuatro archivos distintos) y toda la fase en paralelo con US1–US3
- Polish: T029 y T030 en paralelo

---

## Parallel Example: User Story 4

```bash
Task: "T023 updateMetadata_keepsDraft... en DocumentCommandServiceTest.java"
Task: "T024 findDetailById_returnsWorkflowStatus... en DocumentQueryServiceTest.java"
Task: "T025 toDetailResponse_mapsWorkflowStatus... en DocumentMapperTest.java"
Task: "T026 findById_returnsWorkflowStatus... en DocumentControllerWebMvcTest.java"
```

## Parallel Example: User Story 3

```bash
Task: "T019 uploadSingleForBatch_createsDraft... en DocumentCommandServiceTest.java"
Task: "T020 uploadBatch_includesWorkflowStatusPerItem... en DocumentBatchServiceTest.java"
Task: "T021 uploadBatch_returns200WithWorkflowStatusPerItem... en DocumentBatchControllerWebMvcTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup
2. Phase 2: Foundational (bloquea todo)
3. Phase 3: US1
4. **STOP y VALIDAR**: `./mvnw test` y escenario 2 de `quickstart.md`

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → carga en Borrador (MVP)
3. US2 → la rama Sin flujo verificada (sin regresiones del MVP)
4. US3 → carga múltiple cubierta
5. US4 → estado fijo y visible en ficha/edición
6. US5 → migración validada
7. Polish → CHANGELOG, documentación, `verify`, quickstart, revisión de constitución

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes
- Una sola rama de feature (`feature/hu-33`); hacer commit por fase o grupo lógico
- No tocar listado, búsqueda, dashboard ni `DocumentAccessValidator` (visibilidad = HU-42)
- No crear `document_approval_events` ni el evento MIGRATED (T-03, historias posteriores)
