# Data Model: Carga de documento en estado Borrador (HU-33)

## Tabla `documents` — columnas nuevas

| Columna           | Tipo        | Restricciones                                                                                              | Descripción                                                            |
|-------------------|-------------|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| `workflow_status` | VARCHAR(20) | NOT NULL, DEFAULT `'NOT_REQUIRED'`, `ck_documents_workflow_status` IN (`NOT_REQUIRED`, `DRAFT`, `IN_REVIEW`, `APPROVED`, `ARCHIVED`) | Estado del documento en el flujo de aprobación. Independiente de `status`. |
| `cycle_number`    | INT         | NOT NULL, DEFAULT 0, `ck_documents_cycle_number` (`cycle_number >= 0`)                                      | Ciclos de envío a revisión. Siempre 0 en esta historia (lo incrementa HU-35). |

Índice nuevo: `idx_documents_workflow_status` sobre `documents(workflow_status)` (FR-015, para
HU-36/HU-42).

### Migración `V5__add_workflow_status_to_documents.sql`

1. `ADD COLUMN IF NOT EXISTS workflow_status ...` y `ADD COLUMN IF NOT EXISTS cycle_number ...`.
2. `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT` para ambos `CHECK`.
3. `UPDATE documents SET workflow_status = 'APPROVED' WHERE status = 'ACTIVE' AND workflow_status = 'NOT_REQUIRED'` (D-08).
4. `CREATE INDEX IF NOT EXISTS idx_documents_workflow_status ON documents (workflow_status)`.

Resultado sobre los datos existentes:

| Documento previo      | `workflow_status` | `cycle_number` |
|-----------------------|-------------------|----------------|
| `status = 'ACTIVE'`   | `APPROVED`        | 0              |
| `status = 'DELETED'`  | `NOT_REQUIRED`    | 0              |

No se crea ningún aprobador ni evento: el evento `MIGRATED` llega con la tabla
`document_approval_events` (T-03).

## Entidad `Document` (JPA)

Campos nuevos:

- `DocumentWorkflowStatus workflowStatus`: `@Enumerated(EnumType.STRING)`,
  `@Column(name = "workflow_status", nullable = false, length = 20)`. `@PrePersist` completa
  `NOT_REQUIRED` si llega `null`.
- `int cycleNumber`: `@Column(name = "cycle_number", nullable = false)`, 0 por defecto.

## Enum `DocumentWorkflowStatus` (nuevo, `document/enums`)

```text
NOT_REQUIRED  Sin flujo (comportamiento del MVP)
DRAFT         Borrador
IN_REVIEW     En revisión        (HU-35)
APPROVED      Aprobado           (HU-37, migración D-08)
ARCHIVED      Archivado, final   (HU-39)
```

Método estático `initialFor(boolean requiresApproval)`: `true` → `DRAFT`, `false` → `NOT_REQUIRED`.

### Transiciones en esta historia

| Origen | Destino        | Disparador                                  |
|--------|----------------|---------------------------------------------|
| —      | `DRAFT`        | Carga en categoría con `requires_approval`  |
| —      | `NOT_REQUIRED` | Carga en categoría sin `requires_approval`  |
| —      | `APPROVED`     | Migración V5 de documentos activos (D-08)   |

Ninguna otra operación de esta historia modifica `workflow_status`: la edición de metadatos
(incluido el cambio de categoría), la eliminación lógica y el cambio del indicador de la
categoría lo dejan intacto.

## Bitácora (`activity_log.detail`, acción `UPLOAD`)

| Vía        | Formato                                                          |
|------------|------------------------------------------------------------------|
| Individual | `Archivo: {originalFileName}; workflow_status: {estado}`         |
| Lote       | `Carga múltiple — Archivo: {originalFileName}; workflow_status: {estado}` |

## Categoría (sin cambios)

Se lee `requires_approval` mediante `CategoryQueryService.requiresApproval(categoryId)` (HU-31) en
cada carga.
