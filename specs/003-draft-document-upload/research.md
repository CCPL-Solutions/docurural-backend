# Research: Carga de documento en estado Borrador (HU-33)

Sin `NEEDS CLARIFICATION` en el Technical Context: las dudas de alcance se resolvieron en la spec
(Clarifications, sesión 2026-09-27). Aquí se documentan las decisiones de diseño.

## R1. Dónde se decide el estado inicial del flujo

- **Decision**: `DocumentCommandServiceImpl` inyecta `CategoryQueryService` (HU-31) y, dentro de
  `processSingleFile`, obtiene `requiresApproval(category.getId())`. La traducción booleano →
  estado vive en el enum: `DocumentWorkflowStatus.initialFor(boolean requiresApproval)` (DRAFT o
  NOT_REQUIRED). `upload` y `uploadSingleForBatch` pasan por `processSingleFile`, así que ambas
  vías quedan cubiertas en un único punto.
- **Rationale**: FR-004 exige consultar el valor vigente a través de la consulta de solo lectura
  de HU-31, que se creó justo para esto y no depende de `document` (sin ciclo, D-4). Como
  `CategoryQueryServiceImpl` es `@Transactional(readOnly = true)` y se une a la transacción de la
  carga, `findById` resuelve la categoría ya cargada desde el contexto de persistencia: no hay
  consulta SQL adicional. Poner la regla en el enum evita una clase nueva para un ternario.
- **Alternatives considered**:
  - Leer `category.isRequiresApproval()` de la entidad ya cargada: mismo valor y un colaborador
    menos, pero contradice FR-004 y ahonda la desviación D-4 (uso de `CategoryRepository` desde
    `document`) en lugar de empezar a salir de ella.
  - Un `@Component DocumentWorkflowPolicy` (como `SensitivityPolicy`): añade igualmente un
    colaborador y una clase para una sola expresión. Se reconsiderará en HU-35/HU-37, cuando
    haya transiciones reales que encapsular.

## R2. Modelo del estado del flujo en `documents`

- **Decision**: enum nuevo `DocumentWorkflowStatus { NOT_REQUIRED, DRAFT, IN_REVIEW, APPROVED,
  ARCHIVED }` en `document/enums`, mapeado con `@Enumerated(EnumType.STRING)` a
  `workflow_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED'` con `CHECK`. Campo
  `cycleNumber` (`int`) mapeado a `cycle_number INT NOT NULL DEFAULT 0` con `CHECK (>= 0)`. El
  servicio fija `workflowStatus` explícitamente en el builder; `@PrePersist` completa
  `NOT_REQUIRED` solo como red de seguridad (mismo patrón que `status` y `sensitivityLevel`).
- **Rationale**: la restricción técnica de la constitución exige `VARCHAR + CHECK` para enums. Se
  definen los cinco valores ya (FR-001) para que la migración y el `CHECK` no tengan que cambiar
  en cada historia posterior. El estado del flujo es independiente de `status` (ACTIVE/DELETED).
- **Alternatives considered**: reutilizar `DocumentStatus` añadiendo valores: mezcla vigencia y
  flujo, y rompe el borrado lógico (Principio VII). Crear `cycle_number` en HU-35: obligaría a
  otra migración sobre la misma tabla sin ganar nada; la spec lo pide ya (FR-002).

## R3. Migración y documentos existentes (D-08)

- **Decision**: `V5__add_workflow_status_to_documents.sql` (siguiente versión libre; V1–V4
  intactas). Añade ambas columnas con `IF NOT EXISTS`, recrea los `CHECK` con
  `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT`, ejecuta
  `UPDATE documents SET workflow_status = 'APPROVED' WHERE status = 'ACTIVE' AND workflow_status = 'NOT_REQUIRED'`
  y crea `idx_documents_workflow_status` con `IF NOT EXISTS`. Los eliminados quedan en
  `NOT_REQUIRED` (valor por defecto).
- **Rationale**: aclaración Q1 (opción A). Al hacerlo en la misma migración que crea la columna
  no hay ventana en la que un documento posterior a HU-33 se confunda con uno del MVP. En T-03,
  el evento MIGRATED se podrá generar para "APPROVED sin eventos" sin ambigüedad, porque nadie
  puede aprobar hasta HU-37. La condición extra `workflow_status = 'NOT_REQUIRED'` hace el
  `UPDATE` inocuo si se reejecutara a mano.
- **Alternatives considered**: nombrar la migración V8 como en el documento de HU: dejaría huecos
  sin sentido en la secuencia de Flyway del repositorio.

## R4. Mensaje de confirmación según el estado

- **Decision**: clave nueva `document.uploaded.draft=Documento cargado como borrador`. El
  servicio elige la clave con un método privado `uploadMessageKey(DocumentWorkflowStatus)`:
  `DRAFT` → `document.uploaded.draft`; cualquier otro → `document.uploaded.success` (sin cambios).
- **Rationale**: FR-006 y FR-014 (mensaje externalizado, Principio VIII). Un único punto de
  decisión, sin flags booleanos en firmas públicas (Principio III).
- **Alternatives considered**: guardar la clave de mensaje en el enum: acopla el dominio al i18n.

## R5. Detalle de la entrada UPLOAD en la bitácora

- **Decision**: se conserva el prefijo actual y se añade el estado separado por `; `:
  - Individual: `Archivo: acta.pdf; workflow_status: DRAFT`
  - Lote: `Carga múltiple — Archivo: acta.pdf; workflow_status: NOT_REQUIRED`
- **Rationale**: FR-008 y criterio 7 de la HU. Mantener el prefijo evita romper lecturas actuales
  de la bitácora; el formato `workflow_status: X` es el literal de la HU. Era el punto diferido en
  `/speckit-clarify`.
- **Alternatives considered**: sustituir el detalle solo por el estado: pierde el nombre del
  archivo que hoy se registra.

## R6. Exposición del estado en las respuestas

- **Decision**: añadir `String workflowStatus` (nombre del enum) a `UploadDocumentResponseDto`,
  `BatchUploadItemResultDto` (null en los fallidos), `DocumentDetailResponseDto` y
  `UpdateDocumentMetadataResponseDto`. En el mapper, con la misma expresión
  `enum != null ? enum.name() : null` que ya se usa para `sensitivityLevel`.
  `DocumentSummaryResponseDto` (listado/búsqueda) no cambia.
- **Rationale**: FR-006, FR-007 y FR-007a (aclaración Q3, opción B). El nombre del enum, y no una
  etiqueta traducida, porque la etiqueta con color es de HU-41 y del cliente.
- **Alternatives considered**: exponer el enum tipado en el record: rompe el patrón del proyecto
  (todos los enums salen como `String`).

## R7. El estado no se recalcula al editar

- **Decision**: `applyMetadataUpdates` no toca `workflowStatus`; se añade una prueba que mueve un
  documento DRAFT a una categoría sin aprobación (y a la inversa) y verifica que el estado no
  cambia y que `CategoryQueryService` no se consulta en la edición.
- **Rationale**: FR-010. La garantía es la ausencia de código, así que se asegura con pruebas de
  regresión.

## R8. Visibilidad provisional

- **Decision**: ningún cambio en `DocumentSearchService`, `DocumentAccessValidator` ni en el
  dashboard. Queda registrada la condición de despliegue (no pasar a producción antes de HU-42) en
  la spec y en la entrada de `CHANGELOG.md`.
- **Rationale**: aclaración Q2 (opción A).
