[← Volver al README](../README.md)

# Modelo de datos

Flyway gestiona el versionado del esquema. Las migraciones se encuentran en:

```
src/main/resources/db/migration/
├── V1__init_schema.sql                          # Esquema consolidado: users, categories, documents, activity_log
├── V2__seed_categories.sql                      # Carga las 8 categorías documentales predefinidas
├── V3__add_can_approve_to_users.sql             # users.can_approve + CHECK que lo prohíbe en READER (HU-32)
├── V4__add_requires_approval_to_categories.sql  # categories.requires_approval, false por defecto (HU-31)
└── V5__add_workflow_status_to_documents.sql     # documents.workflow_status + cycle_number; activos → APPROVED (HU-33)
```

El modo DDL de Hibernate es `validate`: **nunca crea ni modifica tablas automáticamente**. Cualquier cambio de
esquema se hace en una nueva migración `V{n}__descripcion.sql`; las migraciones existentes nunca se modifican.

## Entidades y relaciones

| Entidad       | Tabla            |
|---------------|--------------------|
| `User`        | `users`              |
| `Category`    | `categories`           |
| `Document`    | `documents`               |
| `ActivityLog` | `activity_log`               |

Todas las relaciones son `@ManyToOne(fetch = LAZY)` y unidireccionales (no hay colecciones `@OneToMany`):

- `Document.category` → `Category` (obligatoria)
- `Document.uploadedBy` → `User` (obligatoria)
- `Category.createdBy` → `User` (opcional, para permitir el seed de categorías por Flyway)
- `ActivityLog.user` → `User` (obligatoria)
- `ActivityLog.document` → `Document` (opcional; acciones como `LOGIN`/`LOGOUT`/`CREATE_USER` no tienen documento asociado)

## Indicadores del flujo de aprobación

| Tabla.columna                  | Tipo                             | Significado                                                                                              |
|--------------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------|
| `users.can_approve`            | `BOOLEAN NOT NULL DEFAULT FALSE` | El usuario puede aprobar documentos. `CHECK ck_users_can_approve_not_reader`: nunca `true` en `READER`. |
| `categories.requires_approval` | `BOOLEAN NOT NULL DEFAULT FALSE` | Los documentos cargados en la categoría deben pasar por el flujo de aprobación.                         |

Los cambios de ambos indicadores quedan en `activity_log.detail` con el formato `can_approve: a → b` (`EDIT_USER`)
y `requires_approval: a → b` (`EDIT_CATEGORY`).

## Estado del flujo de aprobación de documentos

| Tabla.columna               | Tipo                                         | Significado                                                                                          |
|-----------------------------|----------------------------------------------|------------------------------------------------------------------------------------------------------|
| `documents.workflow_status` | `VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED'` | Estado en el flujo (`CHECK ck_documents_workflow_status`). Independiente de `status` (vigente/eliminado). Índice `idx_documents_workflow_status`. |
| `documents.cycle_number`    | `INT NOT NULL DEFAULT 0`                     | Ciclos de envío a revisión (`CHECK ck_documents_cycle_number`, `>= 0`).                              |

Al cargar, el backend fija `DRAFT` si la categoría tiene `requires_approval = true` en ese momento y `NOT_REQUIRED`
en caso contrario (HU-33); el cliente no puede enviarlo y editar la categoría del documento no lo recalcula. La
entrada `UPLOAD` de `activity_log.detail` añade `; workflow_status: X`. Tras la migración `V5`, los documentos
activos existentes quedan en `APPROVED` (sin aprobador registrado) y los eliminados en `NOT_REQUIRED`.

## Enums de dominio

| Enum                | Valores                                                                                                                                                |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `UserRole`            | `ADMIN`, `EDITOR`, `READER`                                                                                                                              |
| `UserStatus`           | `ACTIVE`, `INACTIVE`                                                                                                                                       |
| `DocumentStatus`        | `ACTIVE`, `DELETED`                                                                                                                                          |
| `DocumentWorkflowStatus` | `NOT_REQUIRED`, `DRAFT`, `IN_REVIEW`, `APPROVED`, `ARCHIVED`                                                                                                |
| `DocumentFormat`         | `PDF`, `DOCX`, `XLSX`, `JPG`, `PNG`                                                                                                                             |
| `CategoryStatus`          | `ACTIVE`, `INACTIVE`                                                                                                                                               |
| `SensitivityLevel`         | `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL` (jerárquico)                                                                                                                 |
| `ActivityAction`            | `LOGIN`, `LOGOUT`, `UPLOAD`, `DOWNLOAD`, `VIEW`, `EDIT_DOC`, `DELETE_DOC`, `CREATE_USER`, `EDIT_USER`, `ACTIVATE_USER`, `DEACTIVATE_USER`, `CREATE_CATEGORY`, `EDIT_CATEGORY`, `ACTIVATE_CATEGORY`, `DEACTIVATE_CATEGORY`, `SEARCH`, `ACCESS_DENIED` |
| `BusinessErrorCode`          | `INVALID_ARGUMENT` (400), `FORBIDDEN` (403), `PAYLOAD_TOO_LARGE` (413), `UNSUPPORTED_MEDIA_TYPE` (415)                                                                  |

## Categorías predefinidas

| Categoría        | Descripción                                                               | Sensibilidad por defecto |
|-------------------|--------------------------------------------------------------------------------|-----------------------------|
| Actas               | Actas de reuniones, consejos directivos, comités                                  | INTERNAL                     |
| Resoluciones          | Resoluciones rectorales y administrativas                                            | INTERNAL                     |
| Matrículas              | Documentos de inscripción y registro de estudiantes                                    | RESTRICTED                   |
| Certificados               | Constancias de estudio, certificados de notas, diplomas                                  | RESTRICTED                   |
| Correspondencia                | Comunicados oficiales enviados y recibidos                                                  | INTERNAL                     |
| Informes                          | Informes pedagógicos, académicos, de gestión y del programa de biotecnología                   | INTERNAL                     |
| Normatividad                         | Manuales de convivencia, PEI, planes de área, protocolos del laboratorio de biotecnología          | INTERNAL                     |
| Otro                                    | Documentos que no corresponden a ninguna categoría anterior                                        | INTERNAL                     |

Tras la migración `V4`, las 8 categorías quedan con `requires_approval = false`; el `ADMIN` decide cuáles activar.
