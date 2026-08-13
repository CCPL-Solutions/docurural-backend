[← Volver al README](../README.md)

# Modelo de datos

Flyway gestiona el versionado del esquema. Las migraciones se encuentran en:

```
src/main/resources/db/migration/
├── V1__init_schema.sql       # Esquema consolidado: users, categories, documents, activity_log
└── V2__seed_categories.sql   # Carga las 8 categorías documentales predefinidas
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

## Enums de dominio

| Enum                | Valores                                                                                                                                                |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `UserRole`            | `ADMIN`, `EDITOR`, `READER`                                                                                                                              |
| `UserStatus`           | `ACTIVE`, `INACTIVE`                                                                                                                                       |
| `DocumentStatus`        | `ACTIVE`, `DELETED`                                                                                                                                          |
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
| Informes                          | Informes pedagógicos, académicos, de gestión                                                  | INTERNAL                     |
| Normatividad                         | Manuales de convivencia, PEI, planes de área, protocolos de laboratorio                          | INTERNAL                     |
| Otro                                    | Documentos que no corresponden a ninguna categoría anterior                                        | INTERNAL                     |
