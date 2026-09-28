# Implementation Plan: Carga de documento en estado Borrador (HU-33)

**Branch**: `feature/hu-33` (directorio de spec: `003-draft-document-upload`) | **Date**: 2026-09-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-draft-document-upload/spec.md`

## Summary

Añadir a `documents` el estado del flujo de aprobación (`workflow_status`, enum
`DocumentWorkflowStatus` con cinco valores) y el contador `cycle_number`. La migración V5 los crea,
pasa los documentos activos existentes a `APPROVED` (D-08) y añade `idx_documents_workflow_status`.
En la carga individual y en la múltiple, `DocumentCommandServiceImpl.processSingleFile` consulta
`CategoryQueryService.requiresApproval` (HU-31) y fija `DRAFT` o `NOT_REQUIRED`. El cliente no
puede enviarlo. La respuesta de la carga incluye el estado y, si es borrador, el mensaje
"Documento cargado como borrador". Cada resultado exitoso del lote, el detalle y la respuesta de
la edición de metadatos exponen `workflowStatus`. La entrada `UPLOAD` de la bitácora añade
`workflow_status: X`. La edición de metadatos no recalcula el estado. El listado, la búsqueda y la
visibilidad no cambian (HU-42).

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 3.5.x, Spring Data JPA, Spring Security (`@PreAuthorize`), MapStruct (mappers abstractos), Lombok, Bean Validation, Flyway

**Storage**: PostgreSQL; nueva migración `V5__add_workflow_status_to_documents.sql`

**Testing**: JUnit 5 + Mockito (`MockitoExtension`, `@Spy` del `DocumentMapper` real), `@WebMvcTest` para los controladores; `TestFixtures`

**Target Platform**: Servicio web REST (JVM, AWS)

**Project Type**: web-service (backend; el aviso del formulario y las etiquetas son del cliente web)

**Performance Goals**: la consulta `requiresApproval` se resuelve desde el contexto de persistencia de la carga (sin SQL extra, R1); índice sobre `workflow_status` para HU-36/HU-42 (búsqueda < 3 s con 10.000 documentos, RF-03)

**Constraints**: cobertura JaCoCo ≥80% líneas / ≥65% ramas; migraciones inmutables (V1–V4 intactas); el cliente no fija el estado (FR-005); la edición no lo recalcula (FR-010)

**Scale/Scope**: hasta ~10.000 documentos; 2 columnas + 1 índice, 1 enum nuevo, 1 clave i18n, ~8 clases de producción modificadas, sin endpoints nuevos

Sin `NEEDS CLARIFICATION` pendientes (ver [research.md](research.md)).

## Constitution Check

*GATE: pasa antes de Phase 0; re-evaluado tras Phase 1.*

| Principio | Estado | Evidencia |
|-----------|--------|-----------|
| I. Módulos por feature | ✅ | `document` pregunta a `category` por su interfaz de solo lectura `CategoryQueryService` (no a `CategoryRepository`), sin ciclo. No se añade ningún uso nuevo de repositorios ajenos; la desviación D-4 existente no crece. |
| II. DI e inmutabilidad | ✅ | Colaborador nuevo por constructor (`@RequiredArgsConstructor`), declarado como interfaz. DTO siguen siendo `record`. |
| III. Simplicidad / fail fast | ⚠️ justificado | Sin métodos nuevos de más de 3 parámetros ni flags booleanos (`initialFor(boolean)` es un factory de valor, no bifurca comportamiento de un servicio). `DocumentCommandServiceImpl` pasa de 10 a 11 colaboradores: ver Complexity Tracking. |
| IV. Contrato de pruebas | ✅ | Se amplían `DocumentCommandServiceTest`, `DocumentBatchServiceTest`, `DocumentMapperTest`, `DocumentControllerWebMvcTest`, `DocumentBatchControllerWebMvcTest`, `DocumentQueryServiceTest`; fixture nuevo `documentDraft(...)` en `TestFixtures`. Nombres `<accion>_<contexto>_<resultado>`. |
| V. Cobertura | ✅ | Ramas nuevas (DRAFT/NOT_REQUIRED en individual y lote, mensaje, detalle de bitácora, edición sin recalcular, mapeo con `null`) con prueba. Sin nuevas exclusiones. |
| VI. Migraciones inmutables | ✅ | V5 nueva e idempotente (`IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS`, `UPDATE` con guarda). Enum como `VARCHAR` + `CHECK`. |
| VII. Borrado lógico | ✅ | `workflow_status` es independiente de `status`; `deleteLogical` no lo toca. |
| VIII. Errores / i18n / auditoría | ✅ | Clave nueva `document.uploaded.draft` vía `MessageResolver`. Sin errores nuevos. La bitácora sigue por `ActivityLogService.record` (REQUIRES_NEW intacto); `AuditContext` ya presente. |
| IX. Seguridad | ✅ | Sin endpoints nuevos ni cambios de reglas (`ADMIN`/`EDITOR` para cargar). Ningún `RequestDto` cambia: el estado no se acepta del cliente. Visibilidad provisional aceptada con condición de despliegue (spec, Q2). |

**Post-diseño**: sin cambios respecto a la tabla.

## Project Structure

### Documentation (this feature)

```text
specs/003-draft-document-upload/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── documents-api.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks (no lo crea este comando)
```

### Source Code (repository root)

```text
src/main/
├── java/co/edu/docurural/document/
│   ├── enums/DocumentWorkflowStatus.java          # NUEVO: 5 valores + initialFor(boolean)
│   ├── entity/Document.java                       # + workflowStatus, cycleNumber; @PrePersist default NOT_REQUIRED
│   ├── dto/UploadDocumentResponseDto.java         # + String workflowStatus
│   ├── dto/BatchUploadItemResultDto.java          # + String workflowStatus (null si falla)
│   ├── dto/DocumentDetailResponseDto.java         # + String workflowStatus
│   ├── dto/UpdateDocumentMetadataResponseDto.java # + String workflowStatus
│   ├── mapper/DocumentMapper.java                 # expresiones enum → String para workflowStatus
│   └── service/
│       ├── DocumentCommandServiceImpl.java        # + CategoryQueryService; estado inicial, mensaje, detalle UPLOAD
│       └── DocumentBatchServiceImpl.java          # resultado por archivo con workflowStatus
└── resources/
    ├── db/migration/V5__add_workflow_status_to_documents.sql
    └── messages.properties                        # + document.uploaded.draft

src/test/java/co/edu/docurural/
├── support/TestFixtures.java                      # + documentDraft(id, category, uploadedBy)
└── document/{service,mapper,controller}/          # pruebas ampliadas

CHANGELOG.md                                       # + entrada Added bajo [Unreleased], con condición de despliegue
```

**Structure Decision**: proyecto único Spring Boot ya existente; la funcionalidad extiende el
paquete `document` (escritura en `DocumentCommandService`, reparto CQRS intacto) y consume la
interfaz de solo lectura de `category` entregada en HU-31. No se crean paquetes nuevos.

## Complexity Tracking

| Desviación | Por qué | Alternativa más simple descartada |
|------------|---------|-----------------------------------|
| `DocumentCommandServiceImpl` pasa de 10 a 11 colaboradores (`+ CategoryQueryService`) | FR-004 exige decidir el estado con la consulta de solo lectura de HU-31, que además es la vía prescrita por D-4 para salir del uso de `CategoryRepository`. La clase ya excedía la guía de 2-3 colaboradores antes de esta historia; partirla (p. ej. separar la carga en un `DocumentUploadService`) es un refactor de alcance propio. **Registrar en la descripción del PR.** | Leer `category.isRequiresApproval()` de la entidad ya cargada: contradice FR-004 y profundiza D-4. Componente `DocumentWorkflowPolicy`: añade igualmente un colaborador para un ternario (R1). |
