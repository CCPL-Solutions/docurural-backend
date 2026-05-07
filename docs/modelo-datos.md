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
┌──────────────┐         ┌────────────────┐
│    users     │         │   categories   │
├──────────────┤         ├────────────────┤
│ id (PK)      │◄──┐     │ id (PK)        │
│ full_name    │   │     │ name           │
│ email        │   │     │ description    │
│ password_hash│   │     │ status         │
│ role         │   │     │ created_at     │
│ status       │   └─────│ created_by(FK) │
│ created_at   │         └───────┬────────┘
│ last_login   │                 │
└──────┬───────┘                 │ category_id (FK)
       │                         │
       │ uploaded_by (FK)        ▼
       │              ┌─────────────────────┐
       │              │      documents      │
       │              ├─────────────────────┤
       │              │ id (PK)             │
       │              │ title               │
       │              │ description         │
       │              │ category_id (FK) ◄──┘
       │              │ responsible_area    │
       │              │ document_date       │
       │              │ file_path           │
       │              │ original_file_name  │
       │              │ file_format         │
       │              │ file_size_bytes     │
       └─────────────►│ uploaded_by (FK)    │
                      │ created_at          │
                      │ status              │
                      └─────────┬───────────┘
                                │
                                │ document_id (FK, nullable)
                                ▼
┌──────────────────────────────────────────┐
│              activity_log                │
├──────────────────────────────────────────┤
│ id (PK)                                  │
│ user_id (FK) ──────────────────────────► users.id
│ action                                   │
│ document_id (FK, nullable) ────────────► documents.id
│ action_timestamp                         │
│ ip_address                               │
│ detail                                   │
└──────────────────────────────────────────┘
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

**Restricciones:**

- `uq_users_email`: unicidad del email.
- `ck_users_role`: rol solo puede ser `ADMIN`, `EDITOR` o `READER`.
- `ck_users_status`: status solo puede ser `ACTIVE` o `INACTIVE`.

**Índices:** `idx_users_role`, `idx_users_status`

---

## Tabla: `categories`

Taxonomía documental predefinida del sistema.

| Columna       | Tipo           | Restricciones                                       | Descripción                                                   |
|---------------|----------------|-----------------------------------------------------|---------------------------------------------------------------|
| `id`          | `BIGSERIAL`    | PK                                                  | Identificador único auto-incremental.                         |
| `name`        | `VARCHAR(100)` | NOT NULL, UNIQUE                                    | Nombre de la categoría.                                       |
| `description` | `TEXT`         | NULL                                                | Descripción de la categoría.                                  |
| `status`      | `VARCHAR(20)`  | NOT NULL DEFAULT 'ACTIVE', CHECK (ACTIVE, INACTIVE) | Estado (borrado lógico).                                      |
| `created_at`  | `TIMESTAMP`    | NOT NULL DEFAULT NOW()                              | Fecha y hora de creación.                                     |
| `created_by`  | `BIGINT`       | NULL, FK → `users.id`                               | Usuario que creó la categoría. NULL para categorías del seed. |

**Restricciones:**

- `uq_categories_name`: unicidad del nombre.
- `ck_categories_status`: status solo puede ser `ACTIVE` o `INACTIVE`.
- `fk_categories_created_by`: referencia al usuario creador.

**Índices:** `idx_categories_name`, `idx_categories_status`

> `created_by` admite `NULL` para desacoplar el seed de categorías de la creación del administrador inicial.

---

## Tabla: `documents`

Metadatos de los documentos cargados al sistema.

| Columna              | Tipo           | Restricciones                                      | Descripción                                         |
|----------------------|----------------|----------------------------------------------------|-----------------------------------------------------|
| `id`                 | `BIGSERIAL`    | PK                                                 | Identificador único auto-incremental.               |
| `title`              | `VARCHAR(255)` | NOT NULL                                           | Título del documento.                               |
| `description`        | `TEXT`         | NULL                                               | Descripción opcional del documento.                 |
| `category_id`        | `BIGINT`       | NOT NULL, FK → `categories.id`                     | Categoría a la que pertenece el documento.          |
| `responsible_area`   | `VARCHAR(100)` | NOT NULL                                           | Área responsable del documento.                     |
| `document_date`      | `DATE`         | NOT NULL                                           | Fecha de emisión o vigencia del documento.          |
| `file_path`          | `VARCHAR(500)` | NOT NULL                                           | Ruta del archivo en el sistema de almacenamiento.   |
| `original_file_name` | `VARCHAR(255)` | NOT NULL                                           | Nombre original del archivo al momento de la carga. |
| `file_format`        | `VARCHAR(20)`  | NOT NULL, CHECK (PDF, DOCX, XLSX, JPG, PNG)        | Formato del archivo.                                |
| `file_size_bytes`    | `BIGINT`       | NOT NULL                                           | Tamaño del archivo en bytes.                        |
| `uploaded_by`        | `BIGINT`       | NOT NULL, FK → `users.id`                          | Usuario que cargó el documento.                     |
| `created_at`         | `TIMESTAMP`    | NOT NULL DEFAULT NOW()                             | Fecha y hora de carga del documento.                |
| `status`             | `VARCHAR(20)`  | NOT NULL DEFAULT 'ACTIVE', CHECK (ACTIVE, DELETED) | Estado del documento (borrado lógico).              |

**Restricciones:**

- `ck_documents_status`: status solo puede ser `ACTIVE` o `DELETED`.
- `ck_documents_file_format`: formato del archivo en la lista permitida.
- `fk_documents_category`: referencia a la categoría.
- `fk_documents_uploaded_by`: referencia al usuario que lo cargó.

**Índices:** `idx_documents_category_id`, `idx_documents_responsible_area`, `idx_documents_uploaded_by`,
`idx_documents_created_at`, `idx_documents_status`

> **Sprint 3 (planificado):** se añadirá un índice GIN con `pg_trgm` sobre `title` y `description` para búsqueda
> full-text eficiente.

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
`CREATE_USER`, `EDIT_USER`, `DEACTIVATE_USER`, `CREATE_CATEGORY`, `EDIT_CATEGORY`, `DEACTIVATE_CATEGORY`.

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

| Índice                              | Tabla          | Columna(s)         | Propósito                                 |
|-------------------------------------|----------------|--------------------|-------------------------------------------|
| `idx_users_role`                    | `users`        | `role`             | Filtrar usuarios por rol.                 |
| `idx_users_status`                  | `users`        | `status`           | Filtrar usuarios activos/inactivos.       |
| `idx_categories_name`               | `categories`   | `name`             | Búsqueda y ordenamiento por nombre.       |
| `idx_categories_status`             | `categories`   | `status`           | Filtrar categorías activas/inactivas.     |
| `idx_documents_category_id`         | `documents`    | `category_id`      | Filtrar documentos por categoría.         |
| `idx_documents_responsible_area`    | `documents`    | `responsible_area` | Filtrar por área responsable.             |
| `idx_documents_uploaded_by`         | `documents`    | `uploaded_by`      | Documentos cargados por un usuario.       |
| `idx_documents_created_at`          | `documents`    | `created_at`       | Ordenamiento cronológico.                 |
| `idx_documents_status`              | `documents`    | `status`           | Filtrar documentos activos/eliminados.    |
| `idx_activity_log_user_id`          | `activity_log` | `user_id`          | Historial de acciones de un usuario.      |
| `idx_activity_log_document_id`      | `activity_log` | `document_id`      | Historial de acciones sobre un documento. |
| `idx_activity_log_action_timestamp` | `activity_log` | `action_timestamp` | Consultas cronológicas del log.           |
| `idx_activity_log_action`           | `activity_log` | `action`           | Filtrar por tipo de acción.               |

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

### `ActivityAction`

| Valor                 | Descripción                                        |
|-----------------------|----------------------------------------------------|
| `LOGIN`               | Inicio de sesión del usuario.                      |
| `LOGOUT`              | Cierre de sesión del usuario.                      |
| `UPLOAD`              | Carga de un nuevo documento.                       |
| `DOWNLOAD`            | Descarga de un documento.                          |
| `VIEW`                | Visualización de un documento.                     |
| `EDIT_DOC`            | Edición de metadatos de un documento.              |
| `DELETE_DOC`          | Eliminación lógica de un documento.                |
| `CREATE_USER`         | Creación de un nuevo usuario.                      |
| `EDIT_USER`           | Edición de datos de un usuario existente.          |
| `DEACTIVATE_USER`     | Cambio de estado de un usuario (activa/desactiva). |
| `CREATE_CATEGORY`     | Creación de una nueva categoría.                   |
| `EDIT_CATEGORY`       | Edición de una categoría existente.                |
| `DEACTIVATE_CATEGORY` | Desactivación de una categoría.                    |

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

