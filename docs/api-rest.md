[← Volver al README](../README.md)

# API REST

**URL base:** `http://localhost:8080/api`

Todos los endpoints, excepto los explícitamente marcados como **Público**, requieren un token JWT válido en la
cabecera:

```
Authorization: Bearer <token>
```

## Autenticación

| Método | Ruta           | Acceso      | Descripción                                                                                                          |
|--------|-----------------|-------------|--------------------------------------------------------------------------------------------------------------------------|
| `POST` | `/auth/login`   | Público     | Autentica con email y contraseña. Devuelve token JWT.                                                                       |
| `POST` | `/auth/logout`  | Autenticado | Registra el cierre de sesión en el log de auditoría (el token no se invalida server-side; el cliente lo descarta).            |

## Usuarios

Todos los endpoints de usuarios requieren rol **`ADMIN`**.

| Método  | Ruta                 | Descripción                                                |
|---------|-----------------------|-----------------------------------------------------------------|
| `GET`   | `/users`              | Lista todos los usuarios. Soporta `sortBy` / `sortDir`.             |
| `GET`   | `/users/{id}`         | Obtiene un usuario por su ID.                                        |
| `POST`  | `/users`              | Crea un nuevo usuario.                                                  |
| `PUT`   | `/users/{id}`         | Actualiza los datos de un usuario existente.                              |
| `PATCH` | `/users/{id}/status`  | Activa o desactiva un usuario.                                               |

## Categorías

| Método  | Ruta                       | Acceso                | Descripción                                                                |
|---------|-----------------------------|-------------------------|---------------------------------------------------------------------------------|
| `GET`   | `/categories`               | `ADMIN`, `EDITOR`        | Lista todas las categorías con conteo de documentos activos.                        |
| `GET`   | `/categories/{id}`          | `ADMIN`, `EDITOR`        | Obtiene el detalle de una categoría por ID.                                            |
| `POST`  | `/categories`                | `ADMIN`                  | Crea una nueva categoría documental.                                                      |
| `PUT`   | `/categories/{id}`           | `ADMIN`                  | Edita el nombre y descripción de una categoría.                                              |
| `PATCH` | `/categories/{id}/status`    | `ADMIN`                  | Activa o desactiva una categoría (soft delete).                                                 |

## Documentos

| Método   | Ruta                          | Acceso                       | Descripción                                                                                                                            |
|----------|---------------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`    | `/documents`                    | `ADMIN`, `EDITOR`, `READER`      | Listado paginado con búsqueda de texto (`q`) y filtros: `categoryId`, `responsibleArea`, `dateFrom`, `dateTo`, `uploadedBy` (solo ADMIN). `size` máx. 50. |
| `GET`    | `/documents/filter-options`     | `ADMIN`, `EDITOR`, `READER`      | Opciones para los selectores del panel de filtros: categorías activas y (solo ADMIN) usuarios activos.                                          |
| `GET`    | `/documents/{id}`               | `ADMIN`, `EDITOR`, `READER`      | Ficha completa de metadatos de un documento activo, incluyendo `fileHash` SHA-256 cuando esté disponible.                                         |
| `PUT`    | `/documents/{id}`               | `ADMIN`, `EDITOR`                | Edita metadatos (`title`, `description`, `categoryId`, `responsibleArea`, `documentDate`). `EDITOR` solo edita documentos propios.                   |
| `DELETE` | `/documents/{id}`               | `ADMIN`                          | Eliminación lógica del documento (`status` → `DELETED`). Registra la acción `DELETE_DOC`.                                                              |
| `GET`    | `/documents/{id}/view`          | `ADMIN`, `EDITOR`, `READER`      | Stream del archivo. PDF/JPG/PNG → `inline`; DOCX/XLSX → `attachment`. Registra la acción `VIEW`.                                                        |
| `GET`    | `/documents/{id}/download`      | `ADMIN`, `EDITOR`, `READER`      | Descarga el archivo con `Content-Disposition: attachment` y nombre original. Registra la acción `DOWNLOAD`.                                                |
| `POST`   | `/documents`                     | `ADMIN`, `EDITOR`                | Carga un documento (`multipart/form-data`, part `file`) con sus metadatos. Máximo 10 MB.                                                                     |
| `POST`   | `/documents/batch`               | `ADMIN`, `EDITOR`                | Carga hasta 5 documentos simultáneamente (part `files`) con metadatos comunes. Devuelve un resultado por archivo.                                              |

> Los endpoints `/view` y `/download` añaden los headers `X-File-Name` y `X-File-Size` en la respuesta.

## Dashboard

| Método | Ruta                | Acceso                      | Descripción                                                                                        |
|--------|-----------------------|--------------------------------|---------------------------------------------------------------------------------------------------------|
| `GET`  | `/dashboard/stats`    | `ADMIN`, `EDITOR`, `READER`      | Totales del repositorio, distribución por categoría y últimos 10 documentos cargados, en una sola llamada. |

## Metadata y salud

| Método | Ruta                | Acceso   | Descripción                                                                                                          |
|--------|-----------------------|-----------|---------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/version`             | Público   | Versión, commit, rama y fecha de build del artefacto en ejecución. `unknown` si se arranca sin el goal `build-info`.  |
| `GET`  | `/actuator/health`     | Público   | Estado de salud de la aplicación (`{"status":"UP"}`).                                                                 |

## Almacenamiento de archivos

Todos los entornos usan S3 por defecto (`DOCURURAL_STORAGE_PROVIDER=s3`). El proveedor `local` está disponible para
desarrollo sin conectividad AWS.

| Proveedor | Ruta del archivo                                                  |
|-----------|------------------------------------------------------------------------|
| `s3`      | `{key-prefix}/{año}/{mes}/{uuid}.{ext}` dentro del bucket                  |
| `local`   | `{base-path}/{año}/{mes}/{uuid}.{ext}` en el sistema de archivos             |

En ambos casos, `documents.file_path` persiste solo la ruta relativa `{año}/{mes}/{uuid}.{ext}` para desacoplar el
registro de la ubicación física. El tipo MIME se valida por contenido real (magic bytes) mediante Apache Tika, no por
la extensión del archivo.

Durante la carga se calcula y persiste `file_hash` (SHA-256 en hexadecimal), usado para verificar que el archivo no
fue alterado. Si el cálculo falla, la carga se completa igualmente y el campo queda en `NULL`.

## Formato de errores

Todos los errores siguen la siguiente estructura (`ApiErrorResponseDto`):

```json
{
  "timestamp": "2026-05-07T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción legible del error",
  "fieldErrors": {
    "campo": "mensaje de validación"
  }
}
```

> `fieldErrors` solo aparece en errores de validación (HTTP 400).
