# Modelo de datos — DocuRural

## Tabla de contenidos

- [Diagrama entidad-relación](#diagrama-entidad-relación)
- [Tabla: users](#tabla-users)
- [Tabla: categories](#tabla-categories)
- [Tabla: documents](#tabla-documents)
- [Tabla: activity_log](#tabla-activity_log)
- [Relaciones entre tablas](#relaciones-entre-tablas)
- [Índices](#índices)
- [Enumeraciones](#enumeraciones)
- [Convenciones del esquema](#convenciones-del-esquema)

---

## Diagrama entidad-relación

```
┌──────────────────┐         ┌──────────────────────────┐
│      users       │         │        categories        │
├──────────────────┤         ├──────────────────────────┤
│ id (PK)          │◄──┐     │ id (PK)                  │
│ full_name        │   │     │ name                     │
│ email            │   │     │ description              │
│ password_hash    │   │     │ status                   │
│ role             │   │     │ created_at               │
│ status           │   └─────│ created_by (FK)          │
│ created_at       │         │ default_sensitivity_level│
│ last_login       │         └──────────┬───────────────┘
│ token_version    │                    │
└────────┬─────────┘                    │ category_id (FK)
         │                              │
         │ uploaded_by (FK)             ▼
         │              ┌───────────────────────────────┐
         │              │           documents           │
         │              ├───────────────────────────────┤
         │              │ id (PK)                       │
         │              │ title                         │
         │              │ description                   │
         │              │ category_id (FK) ◄────────────┘
         │              │ responsible_area              │
         │              │ document_date                 │
         │              │ file_path                     │
         │              │ original_file_name            │
         │              │ file_format                   │
         │              │ file_size_bytes               │
         └─────────────►│ uploaded_by (FK)              │
                        │ created_at                    │
                        │ status                        │
                        │ sensitivity_level             │
                        └──────────────┬────────────────┘
                                       │
                                       │ document_id (FK, nullable)
                                       ▼
┌──────────────────────────────────────────────────────┐
│                     activity_log                     │
├──────────────────────────────────────────────────────┤
│ id (PK)                                              │
│ user_id (FK) ──────────────────────────────────────► users.id
│ action                                               │
│ document_id (FK, nullable) ────────────────────────► documents.id
│ action_timestamp                                     │
│ ip_address                                           │
│ detail                                               │
└──────────────────────────────────────────────────────┘
```

---

## Tabla: `users`

Almacena las cuentas de usuario del sistema.

| Columna         | Tipo           | Restricciones                                       | Descripción                                         |
|-----------------|----------------|-----------------------------------------------------|-----------------------------------------------------|
| `id`            | `BIGSERIAL`    | PK                                                  | Identificador único auto-incremental.               |
| `full_name`     | `VARCHAR(100)` | NOT NULL                                            | Nombre completo del usuario.                        |
| `email`         | `VARCHAR(150)` | NOT NULL, UNIQUE                                    | Correo electrónico. Usado como credencial de login. |
| `password_hash` | `VARCHAR(255)` | NOT NULL                                            | Hash BCrypt de la contraseña.                       |
| `role`          | `VARCHAR(20)`  | NOT NULL, CHECK (ADMIN, EDITOR, READER)             | Rol del usuario en el sistema.                      |
| `status`        | `VARCHAR(20)`  | NOT NULL DEFAULT 'ACTIVE', CHECK (ACTIVE, INACTIVE) | Estado de la cuenta (borrado lógico).               |
| `created_at`    | `TIMESTAMP`    | NOT NULL DEFAULT NOW()                              | Fecha y hora de creación del registro.              |
| `last_login`    | `TIMESTAMP`    | NULL                                                | Fecha y hora del último inicio de sesión exitoso.   |
| `token_version` | `INTEGER`      | NOT NULL DEFAULT 0                                  | Versión del token; se incrementa al cerrar sesión para invalidar JWTs emitidos previamente. |

**Restricciones:**

- `uq_users_email`: unicidad del email.
- `idx_users_email_lower`: índice único funcional sobre `LOWER(email)` para login insensible a mayúsculas.
- `ck_users_role`: rol solo puede ser `ADMIN`, `EDITOR` o `READER`.
- `ck_users_status`: status solo puede ser `ACTIVE` o `INACTIVE`.

**Índices:** `idx_users_role`, `idx_users_status`, `idx_users_email_lower`

---

## Tabla: `categories`

Taxonomía documental predefinida del sistema.

| Columna                     | Tipo           | Restricciones                                                    | Descripción                                                                 |
|-----------------------------|----------------|------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `id`                        | `BIGSERIAL`    | PK                                                               | Identificador único auto-incremental.                                       |
| `name`                      | `VARCHAR(100)` | NOT NULL, UNIQUE                                                 | Nombre de la categoría.                                                     |
| `description`               | `TEXT`         | NULL                                                             | Descripción de la categoría.                                                |
| `status`                    | `VARCHAR(20)`  | NOT NULL DEFAULT 'ACTIVE', CHECK (ACTIVE, INACTIVE)              | Estado (borrado lógico).                                                    |
| `created_at`                | `TIMESTAMP`    | NOT NULL DEFAULT NOW()                                           | Fecha y hora de creación.                                                   |
| `created_by`                | `BIGINT`       | NULL, FK → `users.id`                                            | Usuario que creó la categoría. NULL para categorías del seed.               |
| `default_sensitivity_level` | `VARCHAR(20)`  | NOT NULL DEFAULT 'INTERNAL', CHECK (INTERNAL, RESTRICTED, CONFIDENTIAL) | Nivel de sensibilidad por defecto heredado por los documentos de esta categoría. |

**Restricciones:**

- `uq_categories_name`: unicidad del nombre.
- `ck_categories_status`: status solo puede ser `ACTIVE` o `INACTIVE`.
- `ck_categories_default_sensitivity_level`: debe ser `INTERNAL`, `RESTRICTED` o `CONFIDENTIAL`.
- `fk_categories_created_by`: referencia al usuario creador.

**Índices:** `idx_categories_name`, `idx_categories_status`, `idx_categories_default_sensitivity`

> `created_by` admite `NULL` para desacoplar el seed de categorías de la creación del administrador inicial.

> `default_sensitivity_level` se inicializa a `RESTRICTED` para las categorías *Matrículas* y *Certificados* (datos
> personales de menores, protegidos por la Ley 1581 de 2012).

---

## Tabla: `documents`

Metadatos de los documentos cargados al sistema.

| Columna              | Tipo           | Restricciones                                                    | Descripción                                                               |
|----------------------|----------------|------------------------------------------------------------------|---------------------------------------------------------------------------|
| `id`                 | `BIGSERIAL`    | PK                                                               | Identificador único auto-incremental.                                     |
| `title`              | `VARCHAR(255)` | NOT NULL                                                         | Título del documento.                                                     |
| `description`        | `TEXT`         | NULL                                                             | Descripción opcional del documento.                                       |
| `category_id`        | `BIGINT`       | NOT NULL, FK → `categories.id`                                   | Categoría a la que pertenece el documento.                                |
| `responsible_area`   | `VARCHAR(100)` | NOT NULL                                                         | Área responsable del documento.                                           |
| `document_date`      | `DATE`         | NOT NULL                                                         | Fecha de emisión o vigencia del documento.                                |
| `file_path`          | `VARCHAR(500)` | NOT NULL                                                         | Ruta del archivo en el sistema de almacenamiento.                         |
| `original_file_name` | `VARCHAR(255)` | NOT NULL                                                         | Nombre original del archivo al momento de la carga.                      |
| `file_format`        | `VARCHAR(20)`  | NOT NULL, CHECK (PDF, DOCX, XLSX, JPG, PNG)                      | Formato del archivo.                                                      |
| `file_size_bytes`    | `BIGINT`       | NOT NULL                                                         | Tamaño del archivo en bytes.                                              |
| `uploaded_by`        | `BIGINT`       | NOT NULL, FK → `users.id`                                        | Usuario que cargó el documento.                                           |
| `created_at`         | `TIMESTAMP`    | NOT NULL DEFAULT NOW()                                           | Fecha y hora de carga del documento.                                      |
| `status`             | `VARCHAR(20)`  | NOT NULL DEFAULT 'ACTIVE', CHECK (ACTIVE, DELETED)               | Estado del documento (borrado lógico).                                    |
| `sensitivity_level`  | `VARCHAR(20)`  | NOT NULL DEFAULT 'INTERNAL', CHECK (INTERNAL, RESTRICTED, CONFIDENTIAL) | Nivel de sensibilidad del documento; controla quién puede descargarlo. |

**Restricciones:**

- `ck_documents_status`: status solo puede ser `ACTIVE` o `DELETED`.
- `ck_documents_file_format`: formato del archivo en la lista permitida.
- `ck_documents_sensitivity_level`: debe ser `INTERNAL`, `RESTRICTED` o `CONFIDENTIAL`.
- `fk_documents_category`: referencia a la categoría.
- `fk_documents_uploaded_by`: referencia al usuario que lo cargó.

**Índices:** `idx_documents_category_id`, `idx_documents_responsible_area`, `idx_documents_uploaded_by`,
`idx_documents_created_at`, `idx_documents_status`, `idx_documents_document_date`,
`idx_documents_sensitivity_level`

> **V5:** se añadió `idx_documents_document_date` para acelerar los filtros por rango de fecha (`dateFrom` / `dateTo`)
> del endpoint SRC-01. La búsqueda por texto libre se implementa con `ILIKE` (sin `pg_trgm`); si en el futuro el
> volumen supera los 10 000 registros se recomienda un índice GIN con `pg_trgm` sobre `title` y `description`.

> **V7:** `sensitivity_level` se inicializa heredando `default_sensitivity_level` de la categoría para los documentos
> preexistentes activos. El nivel de acceso para descarga se resuelve según la jerarquía `INTERNAL < RESTRICTED < CONFIDENTIAL`.

---

## Tabla: `activity_log`

Registro de auditoría de todas las acciones realizadas por los usuarios.

| Columna            | Tipo          | Restricciones             | Descripción                                                  |
|--------------------|---------------|---------------------------|--------------------------------------------------------------|
| `id`               | `BIGSERIAL`   | PK                        | Identificador único auto-incremental.                        |
| `user_id`          | `BIGINT`      | NOT NULL, FK → `users.id` | Usuario que realizó la acción.                               |
| `action`           | `VARCHAR(50)` | NOT NULL                  | Tipo de acción (ver enum `ActivityAction`).                  |
| `document_id`      | `BIGINT`      | NULL, FK → `documents.id` | Documento relacionado (solo para acciones sobre documentos). |
| `action_timestamp` | `TIMESTAMP`   | NOT NULL DEFAULT NOW()    | Fecha y hora exacta de la acción.                            |
| `ip_address`       | `VARCHAR(45)` | NULL                      | IP del cliente (soporta IPv4 e IPv6).                        |
| `detail`           | `TEXT`        | NULL                      | Información adicional específica de la acción.               |

**Restricciones:**

- `fk_activity_log_user`: referencia al usuario.
- `fk_activity_log_document`: referencia al documento (si aplica).

**Índices:** `idx_activity_log_user_id`, `idx_activity_log_document_id`, `idx_activity_log_action_timestamp`,
`idx_activity_log_action`

> `document_id` es `NULL` para acciones que no están relacionadas con un documento específico: `LOGIN`, `LOGOUT`,
`CREATE_USER`, `EDIT_USER`, `ACTIVATE_USER`, `DEACTIVATE_USER`, `CREATE_CATEGORY`, `EDIT_CATEGORY`,
`ACTIVATE_CATEGORY`, `DEACTIVATE_CATEGORY`, `SEARCH`.

---

## Relaciones entre tablas

| Tabla origen   | FK            | Tabla destino | Cardinalidad | Obligatoria |
|----------------|---------------|---------------|--------------|-------------|
| `categories`   | `created_by`  | `users`       | N:1          | No (NULL)   |
| `documents`    | `category_id` | `categories`  | N:1          | Sí          |
| `documents`    | `uploaded_by` | `users`       | N:1          | Sí          |
| `activity_log` | `user_id`     | `users`       | N:1          | Sí          |
| `activity_log` | `document_id` | `documents`   | N:1          | No (NULL)   |

---

## Índices

| Índice                              | Tabla          | Columna(s)                    | Migración | Propósito                                                         |
|-------------------------------------|----------------|-------------------------------|-----------|-------------------------------------------------------------------|
| `idx_users_role`                    | `users`        | `role`                        | V1        | Filtrar usuarios por rol.                                         |
| `idx_users_status`                  | `users`        | `status`                      | V1        | Filtrar usuarios activos/inactivos.                               |
| `idx_users_email_lower`             | `users`        | `LOWER(email)` (funcional)    | V4        | Login insensible a mayúsculas; garantiza unicidad de email.       |
| `idx_categories_name`               | `categories`   | `name`                        | V1        | Búsqueda y ordenamiento por nombre.                               |
| `idx_categories_status`             | `categories`   | `status`                      | V1        | Filtrar categorías activas/inactivas.                             |
| `idx_categories_default_sensitivity`| `categories`   | `default_sensitivity_level`   | V6        | Filtrar categorías por nivel de sensibilidad por defecto.         |
| `idx_documents_category_id`         | `documents`    | `category_id`                 | V1        | Filtrar documentos por categoría (SRC-01).                        |
| `idx_documents_responsible_area`    | `documents`    | `responsible_area`            | V1        | Filtrar por área responsable.                                     |
| `idx_documents_uploaded_by`         | `documents`    | `uploaded_by`                 | V1        | Documentos cargados por un usuario (SRC-01).                      |
| `idx_documents_created_at`          | `documents`    | `created_at`                  | V1        | Ordenamiento cronológico (default sort de SRC-01).                |
| `idx_documents_status`              | `documents`    | `status`                      | V1        | Filtrar documentos activos/eliminados.                            |
| `idx_documents_document_date`       | `documents`    | `document_date`               | V5        | Filtros por rango de fecha en SRC-01 (HU-21).                     |
| `idx_documents_sensitivity_level`   | `documents`    | `sensitivity_level`           | V7        | Filtrar documentos por nivel de sensibilidad (HU-28).             |
| `idx_activity_log_user_id`          | `activity_log` | `user_id`                     | V1        | Historial de acciones de un usuario.                              |
| `idx_activity_log_document_id`      | `activity_log` | `document_id`                 | V1        | Historial de acciones sobre un documento.                         |
| `idx_activity_log_action_timestamp` | `activity_log` | `action_timestamp`            | V1        | Consultas cronológicas del log.                                   |
| `idx_activity_log_action`           | `activity_log` | `action`                      | V1        | Filtrar por tipo de acción.                                       |

---

## Enumeraciones

### `UserRole`

| Valor    | Descripción                                                    |
|----------|----------------------------------------------------------------|
| `ADMIN`  | Acceso completo: gestión de usuarios, documentos y categorías. |
| `EDITOR` | Puede cargar, editar y eliminar documentos.                    |
| `READER` | Solo puede consultar y descargar documentos.                   |

### `UserStatus` / `CategoryStatus`

| Valor      | Descripción               |
|------------|---------------------------|
| `ACTIVE`   | Entidad activa y visible. |
| `INACTIVE` | Borrado lógico.           |

### `DocumentStatus`

| Valor     | Descripción                      |
|-----------|----------------------------------|
| `ACTIVE`  | Documento activo y accesible.    |
| `DELETED` | Documento eliminado lógicamente. |

### `DocumentFormat`

| Valor  | Descripción            |
|--------|------------------------|
| `PDF`  | Documento PDF.         |
| `DOCX` | Documento Word.        |
| `XLSX` | Hoja de cálculo Excel. |
| `JPG`  | Imagen JPEG.           |
| `PNG`  | Imagen PNG.            |

### `SensitivityLevel`

Jerarquía ordenada de menor a mayor restricción: `INTERNAL < RESTRICTED < CONFIDENTIAL`.

| Valor          | Descripción                                                                                      |
|----------------|--------------------------------------------------------------------------------------------------|
| `INTERNAL`     | Documento de uso interno general. Accesible por cualquier usuario autenticado.                   |
| `RESTRICTED`   | Contiene datos personales o información sensible. Solo `EDITOR` y `ADMIN` pueden descargarlo.    |
| `CONFIDENTIAL` | Información de acceso estrictamente limitado. Solo `ADMIN` puede descargarlo.                    |

> Aplica tanto a `categories.default_sensitivity_level` (herencia al crear un documento) como a
> `documents.sensitivity_level` (nivel definitivo del documento, editable por `ADMIN`).

### `ActivityAction`

| Valor                 | Descripción                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------|
| `LOGIN`               | Inicio de sesión del usuario.                                                                 |
| `LOGOUT`              | Cierre de sesión del usuario.                                                                 |
| `UPLOAD`              | Carga de un nuevo documento.                                                                  |
| `DOWNLOAD`            | Descarga de un documento.                                                                     |
| `VIEW`                | Visualización de un documento.                                                                |
| `EDIT_DOC`            | Edición de metadatos de un documento.                                                         |
| `DELETE_DOC`          | Eliminación lógica de un documento.                                                           |
| `CREATE_USER`         | Creación de un nuevo usuario.                                                                 |
| `EDIT_USER`           | Edición de datos de un usuario existente.                                                     |
| `ACTIVATE_USER`       | Activación de un usuario previamente inactivo.                                                |
| `DEACTIVATE_USER`     | Desactivación de un usuario activo.                                                           |
| `CREATE_CATEGORY`     | Creación de una nueva categoría.                                                              |
| `EDIT_CATEGORY`       | Edición de una categoría existente.                                                           |
| `ACTIVATE_CATEGORY`   | Reactivación de una categoría previamente inactiva.                                           |
| `DEACTIVATE_CATEGORY` | Desactivación de una categoría activa.                                                        |
| `SEARCH`              | Búsqueda de documentos por texto libre. Solo se registra cuando el parámetro `q` está presente. |

---

## Convenciones del esquema

- **Borrado lógico:** Ningún registro se elimina físicamente. Se usa el campo `status` para marcar entidades como
  inactivas o eliminadas.
- **Enums como VARCHAR + CHECK:** Los tipos enumerados se modelan como `VARCHAR` con restricción `CHECK` para mantener
  legibilidad directa en la base de datos y facilitar consultas SQL sin JOIN.
- **Timestamps en UTC:** La propiedad `hibernate.jdbc.time_zone=UTC` garantiza que todos los timestamps se almacenen y
  recuperen en UTC independientemente de la zona horaria del servidor.
- **Naming strategy:** Hibernate convierte automáticamente los nombres de campos camelCase de Java a snake_case en la
  base de datos (`CamelCaseToUnderscoresNamingStrategy`).
- **IDs auto-incrementales:** Toda tabla usa `BIGSERIAL` (equivalente a `BIGINT GENERATED ALWAYS AS IDENTITY`) para
  garantizar identificadores únicos y eficientes.
- **Emails en minúsculas:** A partir de V4, todos los emails se normalizan a minúsculas antes de persistirse; el índice
  funcional `idx_users_email_lower` garantiza unicidad insensible a mayúsculas.
- **Sensibilidad heredada:** Al crear un documento, `sensitivity_level` se inicializa con el valor
  `default_sensitivity_level` de su categoría. El valor puede ajustarse manualmente por un `ADMIN` con posterioridad.