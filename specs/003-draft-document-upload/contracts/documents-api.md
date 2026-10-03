# Contrato: API de documentos, campo `workflowStatus` (HU-33)

No hay endpoints nuevos ni cambios de autorización. Los requests no cambian: el estado del flujo
no se acepta del cliente (FR-005) y cualquier campo extra se ignora. Los ejemplos muestran solo
lo que cambia.

## Campo nuevo

| Campo | Tipo JSON | Valores | Dónde |
|-------|-----------|---------|-------|
| `workflowStatus` | string | `NOT_REQUIRED`, `DRAFT`, `IN_REVIEW`, `APPROVED`, `ARCHIVED` | respuestas de carga individual, de cada archivo exitoso del lote, del detalle y de la edición de metadatos |

`GET /api/documents` y `GET /api/documents/search` **no** cambian (HU-42).

## `POST /api/documents`, carga individual (ADMIN, EDITOR)

Response `201` en una categoría con "Requiere aprobación":

```json
{
  "id": 48,
  "title": "Acta Consejo Directivo Marzo 2026",
  "category": "Actas",
  "workflowStatus": "DRAFT",
  "message": "Documento cargado como borrador"
}
```

Response `201` en una categoría sin aprobación:

```json
{
  "workflowStatus": "NOT_REQUIRED",
  "message": "Documento cargado exitosamente"
}
```

Errores: los mismos de hoy (400 validación/archivo, 403 rol o sensibilidad, 404 categoría
inexistente o inactiva, 413/415 archivo).

## `POST /api/documents/batch`, carga múltiple (ADMIN, EDITOR)

Response `200`, cada elemento de `results` añade `workflowStatus`:

```json
{
  "totalReceived": 2,
  "totalSuccessful": 1,
  "totalFailed": 1,
  "results": [
    { "fileName": "acta_enero.pdf", "success": true, "documentId": 48,
      "workflowStatus": "DRAFT", "errorMessage": null },
    { "fileName": "virus.exe", "success": false, "documentId": null,
      "workflowStatus": null, "errorMessage": "Formato de archivo no permitido" }
  ]
}
```

`workflowStatus` es `null` en los archivos fallidos.

## `GET /api/documents/{id}`, detalle (reglas de acceso actuales)

Response `200` añade `"workflowStatus": "DRAFT"` (o el valor vigente, cualquiera de los cinco).

## `PUT /api/documents/{id}`, edición de metadatos (reglas actuales)

Response `200` añade `"workflowStatus"` con el valor vigente, que **no** cambia aunque la edición
cambie la categoría.

## Bitácora

Entrada `UPLOAD` por cada documento creado:

- Individual: `Archivo: acta_enero.pdf; workflow_status: DRAFT`
- Lote: `Carga múltiple — Archivo: acta_enero.pdf; workflow_status: DRAFT`
