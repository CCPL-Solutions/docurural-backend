# Quickstart: validar HU-33 (carga en estado Borrador)

Contrato de referencia: [contracts/documents-api.md](contracts/documents-api.md). Modelo:
[data-model.md](data-model.md).

## 1. Puerta de calidad

```bash
./mvnw clean verify
```

Debe terminar en verde con los umbrales de JaCoCo (≥80% líneas, ≥65% ramas). Pruebas clave:
`DocumentCommandServiceTest`, `DocumentBatchServiceTest`, `DocumentMapperTest`,
`DocumentControllerWebMvcTest`, `DocumentBatchControllerWebMvcTest`.

## 2. Migración en local

```bash
./mvnw spring-boot:run
```

Con una base que ya tenga documentos, al arrancar Flyway aplica `V5`. Comprobar en PostgreSQL:

```sql
SELECT status, workflow_status, cycle_number, COUNT(*)
FROM documents GROUP BY status, workflow_status, cycle_number;
```

Esperado: activos → `APPROVED`/0; eliminados → `NOT_REQUIRED`/0. Existe
`idx_documents_workflow_status`.

## 3. Escenarios manuales (Swagger o cliente HTTP, con token de ADMIN o EDITOR)

| # | Paso | Resultado esperado |
|---|------|--------------------|
| 1 | Como ADMIN, `PUT /api/categories/{id}` con `"requiresApproval": true` en "Actas" | Categoría con aprobación |
| 2 | `POST /api/documents` en "Actas" | `201`, `workflowStatus: DRAFT`, mensaje "Documento cargado como borrador" |
| 3 | `POST /api/documents` en una categoría sin aprobación | `201`, `workflowStatus: NOT_REQUIRED`, mensaje "Documento cargado exitosamente" |
| 4 | `POST /api/documents/batch` con 2 PDF válidos y 1 archivo inválido en "Actas" | 2 resultados con `DRAFT`, el fallido con `workflowStatus: null` |
| 5 | Enviar `workflowStatus: "APPROVED"` como campo extra en la carga del paso 2 | Se ignora; el documento queda `DRAFT` |
| 6 | `PUT /api/documents/{id}` del documento del paso 2, moviéndolo a una categoría sin aprobación | `200`, `workflowStatus` sigue `DRAFT`; `GET /api/documents/{id}` igual |
| 7 | Desactivar "Requiere aprobación" en "Actas" y volver a cargar | El documento nuevo queda `NOT_REQUIRED`; el del paso 2 sigue `DRAFT` |
| 8 | Consultar `activity_log` de los pasos 2 y 4 | `detail` = `Archivo: …; workflow_status: DRAFT` / `Carga múltiple — Archivo: …; workflow_status: DRAFT` |
| 9 | Revisar `file_hash` de los documentos del paso 2 | Hash SHA-256 de 64 caracteres, como en cualquier carga |

## 4. Recordatorio de despliegue

Los borradores aún se ven con las reglas actuales de sensibilidad. La rama de la v2.0 no debe
pasar a producción antes de HU-42.
