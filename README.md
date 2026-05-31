# DocuRural — Backend

API REST para la gestión documental de instituciones educativas rurales. Permite administrar usuarios, categorías y
documentos con trazabilidad completa de cada acción realizada en el sistema.

---

## Tabla de contenidos

- [Stack tecnológico](#stack-tecnológico)
- [Requisitos previos](#requisitos-previos)
- [Configuración del entorno](#configuración-del-entorno)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Estructura del proyecto](#estructura-del-proyecto)
- [API REST](#api-rest)
- [Seguridad](#seguridad)
- [Base de datos](#base-de-datos)
- [Pruebas](#pruebas)
- [Documentación adicional](#documentación-adicional)

---

## Stack tecnológico

| Componente        | Tecnología                             |
|-------------------|----------------------------------------|
| Lenguaje          | Java 17                                |
| Framework         | Spring Boot 3.5                        |
| Seguridad         | Spring Security 6 + JWT (Auth0 HS256)  |
| Persistencia      | Spring Data JPA + Hibernate            |
| Base de datos     | PostgreSQL 15+                         |
| Migraciones       | Flyway                                 |
| Documentación API | SpringDoc OpenAPI 2 (Swagger UI)       |
| Utilidades        | Lombok                                 |
| Pruebas           | JUnit 5, Mockito, Spring Security Test |
| Cobertura         | JaCoCo (mínimo 80% de líneas)          |
| Validación MIME   | Apache Tika 2.9                        |
| Almacenamiento    | AWS S3 (vía AWS SDK v2)                |
| Secretos          | AWS Parameter Store (spring-cloud-aws) |
| Build             | Maven Wrapper (`mvnw`)                 |

---

## Requisitos previos

- **JDK 17** o superior
- **PostgreSQL 15+** en ejecución
- **Maven** (o usar el wrapper incluido `./mvnw`)
- **AWS CLI** configurado con un perfil que tenga acceso a S3 y Parameter Store (entornos `qa` y `prod`)

---

## Configuración del entorno

Copia el archivo de ejemplo y completa las variables:

```bash
cp .env.example .env
```

| Variable                          | Descripción                                             | Valor por defecto       |
|-----------------------------------|---------------------------------------------------------|-------------------------|
| `DB_HOST`                         | Host de PostgreSQL                                      | `localhost`             |
| `DB_PORT`                         | Puerto de PostgreSQL                                    | `5432`                  |
| `DB_NAME`                         | Nombre de la base de datos                              | `docurural_db`          |
| `DB_USER`                         | Usuario de la base de datos                             | `docurural`             |
| `DB_PASSWORD`                     | Contraseña del usuario de BD                            | —                       |
| `JWT_SECRET`                      | Clave secreta para firmar tokens JWT (mínimo 32 bytes)  | —                       |
| `JWT_EXPIRATION_MS`               | Tiempo de vida del token en milisegundos                | `1800000` (30 min)      |
| `JWT_ISSUER`                      | Emisor incluido en el claim `iss` del JWT               | `docurural`             |
| `SPRING_PROFILES_ACTIVE`          | Perfil activo (`dev`, `qa` o `prod`)                    | `dev`                   |
| `CORS_ALLOWED_ORIGINS`            | Orígenes permitidos en CORS                             | `http://localhost:4200` |
| `ADMIN_SEED_EMAIL`                | Email del administrador inicial (opcional, idempotente) | —                       |
| `ADMIN_SEED_PASSWORD`             | Contraseña del administrador inicial (opcional)         | —                       |
| `DOCURURAL_STORAGE_PROVIDER`      | Proveedor de almacenamiento (`local` o `s3`)            | `s3`                    |
| `DOCURURAL_STORAGE_BASE_PATH`     | Directorio base para archivos locales                   | `./uploads/documents`   |
| `DOCURURAL_STORAGE_S3_BUCKET`     | Nombre del bucket S3                                    | —                       |
| `AWS_REGION`                      | Región AWS                                              | `us-east-1`             |
| `DOCURURAL_STORAGE_S3_KEY_PREFIX` | Prefijo de llaves en S3                                 | `documents`             |
| `DOCURURAL_BCRYPT_STRENGTH`       | Factor de coste de BCrypt                               | `12`                    |

### Perfiles de Spring

Los perfiles se configuran en `application-<perfil>.yaml` y se activan con `SPRING_PROFILES_ACTIVE`.

| Perfil  | Descripción                                                                                          |
|---------|------------------------------------------------------------------------------------------------------|
| `dev`   | Logs SQL + nivel `DEBUG`. Sin Parameter Store; secretos vienen del `.env`.                          |
| `qa`    | Entorno de certificación. Importa secretos de `/docurural/qa/` en Parameter Store.                  |
| `prod`  | Producción. Importa secretos de `/docurural/prod/` en Parameter Store. Swagger UI deshabilitado.    |

La resolución de secretos sigue un orden de prioridad: **Parameter Store → variable de entorno → valor por defecto**.
En `dev`, donde no hay Parameter Store configurado, basta con definir las variables en el `.env`.

---

## Instalación y ejecución

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd docurural-backend

# 2. Compilar el proyecto
./mvnw clean install

# 3. Ejecutar la aplicación
./mvnw spring-boot:run
```

La aplicación arrancará en `http://localhost:8080/api`.

### Swagger UI

Una vez en ejecución, la documentación interactiva de la API está disponible en:

```
http://localhost:8080/api/swagger-ui.html
```

La especificación OpenAPI en formato JSON se puede obtener en:

```
http://localhost:8080/api/v3/api-docs
```

> Swagger UI está deshabilitado en el perfil `prod`.

---

## Estructura del proyecto

El proyecto sigue una organización **package-by-feature** para mejorar la cohesión y la navegabilidad:

```
src/main/java/co/edu/docurural/
├── auth/                  # Autenticación (AUTH-01, AUTH-02)
│   ├── controller/        → AuthController
│   ├── dto/               → LoginRequest, LoginResponse, LogoutResponse, UserSummary
│   └── service/           → AuthService
│
├── user/                  # Gestión de usuarios (USR-01..USR-05)
│   ├── controller/        → UserController
│   ├── dto/               → CreateUserRequest/Response, UpdateUserRequest/Response,
│   │                           UpdateStatusRequest/Response, UserResponse, UserListResponse
│   ├── mapper/            → UserMapper
│   └── service/           → UserService
│
├── document/              # Gestión y búsqueda de documentos (DOC-01..DOC-08 / HU-09..HU-15, HU-20..HU-22)
│   ├── controller/        → DocumentController
│   ├── dto/               → DocumentListResponse, DocumentSummaryResponse, DocumentDetailResponse,
│   │                          ActiveFiltersResponse, FilterOptionsResponse, DocumentFileContent,
│   │                          UploadDocumentRequest, UploadDocumentResponse,
│   │                          BatchUploadDocumentRequest/Response, BatchUploadItemResult,
│   │                          UpdateDocumentMetadataRequest/Response, DeleteDocumentResponse
│   ├── entity/            → Document
│   ├── enums/             → DocumentFormat, DocumentStatus
│   ├── mapper/            → DocumentMapper
│   ├── repository/        → DocumentRepository (+ JpaSpecificationExecutor),
│   │                          DocumentSpecifications, projection/CategoryDocumentCount
│   ├── service/           → DocumentCommandService, DocumentQueryService,
│   │                          DocumentSearchService, DocumentContentService,
│   │                          DocumentBatchService, FileValidationService
│   └── storage/           → FileStorageService, LocalFileStorageService, S3FileStorageService,
│                              StorageProperties, StoredFile
│
├── category/              # Categorías documentales (CAT-01..CAT-05 / HU-16..HU-19)
│   ├── controller/        → CategoryController
│   ├── dto/               → CategoryDetailResponse, CategoryListResponse,
│   │                           CreateCategoryRequest/Response, UpdateCategoryRequest/Response,
│   │                           UpdateCategoryStatusRequest/Response
│   ├── entity/            → Category
│   ├── enums/             → CategoryStatus
│   ├── mapper/            → CategoryMapper
│   ├── repository/        → CategoryRepository
│   └── service/           → CategoryService
│
├── activitylog/           # Auditoría de acciones
│   ├── entity/            → ActivityLog
│   ├── enums/             → ActivityAction
│   ├── repository/        → ActivityLogRepository
│   └── service/           → ActivityLogService
│
├── dashboard/             # Panel de control — endpoint agregado (DSH-01 / HU-24..HU-27)
│   ├── controller/        → DashboardController
│   ├── dto/               → DashboardStatsResponse, SummaryResponse, TopCategoryResponse,
│   │                          CategoryDistributionItemResponse, RecentDocumentResponse
│   └── service/           → DashboardService
│
└── shared/                # Código compartido entre módulos
    ├── config/            → SecurityConfig, CorsConfig, OpenApiConfig,
    │                          InitialAdminSeederConfig
    ├── security/          → JwtTokenProvider, JwtAuthenticationFilter,
    │                          CustomUserDetailsService, CustomUserPrincipal,
    │                          JwtProperties, SecurityConstants
    ├── domain/            → User (entidad), UserRole, UserStatus, UserRepository
    ├── exception/         → GlobalExceptionHandler, BusinessRuleException,
    │                          ConflictException, ResourceNotFoundException,
    │                          BusinessErrorCode
    ├── audit/             → AuditContext, AuditContextResolver, ClientIpResolver
    ├── dto/               → ApiErrorResponse, MessageResponse
    └── util/              → MessageResolver (wrapper i18n)
```

---

## API REST

**URL base:** `http://localhost:8080/api`

Todos los endpoints, excepto `POST /auth/login`, requieren un token JWT válido en la cabecera:

```
Authorization: Bearer <token>
```

### Autenticación

| Método | Ruta           | Acceso      | Descripción                                           |
|--------|----------------|-------------|-------------------------------------------------------|
| `POST` | `/auth/login`  | Público     | Autentica con email y contraseña. Devuelve token JWT. |
| `POST` | `/auth/logout` | Autenticado | Registra el cierre de sesión en el log de auditoría.  |

### Usuarios

Todos los endpoints de usuarios requieren rol **`ADMIN`**.

| Método  | Ruta                 | Descripción                                           |
|---------|----------------------|-------------------------------------------------------|
| `GET`   | `/users`             | Lista todos los usuarios con soporte de ordenamiento. |
| `GET`   | `/users/{id}`        | Obtiene un usuario por su ID.                         |
| `POST`  | `/users`             | Crea un nuevo usuario.                                |
| `PUT`   | `/users/{id}`        | Actualiza los datos de un usuario existente.          |
| `PATCH` | `/users/{id}/status` | Activa o desactiva un usuario.                        |

### Categorías

Todos los endpoints de categorías requieren rol **`ADMIN`**.

| Método  | Ruta                      | HU    | Descripción                                                  |
|---------|---------------------------|-------|--------------------------------------------------------------|
| `GET`   | `/categories`             | HU-19 | Lista todas las categorías con conteo de documentos activos. |
| `GET`   | `/categories/{id}`        | HU-19 | Obtiene el detalle de una categoría por ID.                  |
| `POST`  | `/categories`             | HU-16 | Crea una nueva categoría documental.                         |
| `PUT`   | `/categories/{id}`        | HU-17 | Edita el nombre y descripción de una categoría.              |
| `PATCH` | `/categories/{id}/status` | HU-18 | Activa o desactiva una categoría (soft delete).              |

### Documentos

| Método   | Ruta                        | Acceso                      | HU             | Descripción                                                                                                                                        |
|----------|-----------------------------|-----------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`    | `/documents`                | `ADMIN`, `EDITOR`, `READER` | HU-15/20/21/22 | Lista paginada con búsqueda y filtrado. Ver sección [Búsqueda y filtrado](#búsqueda-y-filtrado-de-documentos).                                      |
| `GET`    | `/documents/filter-options` | `ADMIN`, `EDITOR`, `READER` | HU-21          | Opciones para los selectores del panel de filtros: categorías activas y (solo ADMIN) usuarios activos.                                             |
| `GET`    | `/documents/{id}`           | `ADMIN`, `EDITOR`, `READER` | HU-11/HU-30    | Retorna la ficha completa de metadatos de un documento activo, incluyendo `fileHash` SHA-256 cuando esté disponible (DOC-02).                      |
| `PUT`    | `/documents/{id}`           | `ADMIN`, `EDITOR`           | HU-13          | Edita metadatos (`title`, `description`, `categoryId`, `responsibleArea`, `documentDate`). `EDITOR` solo puede editar documentos propios (DOC-05). |
| `DELETE` | `/documents/{id}`           | `ADMIN`                     | HU-14          | Eliminación lógica del documento (status → DELETED). Registra acción `DELETE_DOC`.                                                                 |
| `GET`    | `/documents/{id}/view`      | `ADMIN`, `EDITOR`, `READER` | HU-11          | Stream del archivo. PDF/JPG/PNG → `inline`; DOCX/XLSX → `attachment` (DOC-07). Registra acción `VIEW`.                                             |
| `GET`    | `/documents/{id}/download`  | `ADMIN`, `EDITOR`, `READER` | HU-12          | Descarga el archivo con `Content-Disposition: attachment` y nombre original (DOC-08). Registra acción `DOWNLOAD`.                                  |
| `POST`   | `/documents`                | `ADMIN`, `EDITOR`           | HU-09          | Carga un documento (`multipart/form-data`) con sus metadatos. Máximo 10 MB.                                                                        |
| `POST`   | `/documents/batch`          | `ADMIN`, `EDITOR`           | HU-10          | Carga hasta 5 documentos simultáneamente con metadatos comunes.                                                                                    |

### Dashboard

| Método | Ruta               | Acceso                      | HU             | Descripción                                                                                                   |
|--------|--------------------|-----------------------------|----------------|---------------------------------------------------------------------------------------------------------------|
| `GET`  | `/dashboard/stats` | `ADMIN`, `EDITOR`, `READER` | HU-24/25/26/27 | Retorna en una sola llamada totales del repositorio, distribución por categoría y últimos 10 documentos (DSH-01). |

### Almacenamiento de archivos

Todos los entornos usan S3 por defecto (`DOCURURAL_STORAGE_PROVIDER=s3`). El proveedor `local` está disponible para
escenarios sin conectividad AWS.

| Proveedor | Ruta del archivo                                                   |
|-----------|--------------------------------------------------------------------|
| `s3`      | `{key-prefix}/{año}/{mes}/{uuid}.{ext}` dentro del bucket          |
| `local`   | `{base-path}/{año}/{mes}/{uuid}.{ext}` en el sistema de archivos   |

En ambos casos, `documents.file_path` persiste solo la ruta relativa `{año}/{mes}/{uuid}.{ext}` para desacoplar el
registro de la ubicación física. El tipo MIME se valida por contenido real (magic bytes) mediante Apache Tika.

Durante la carga se calcula y persiste `file_hash` (SHA-256 en hexadecimal). Si el cálculo falla, la carga se completa
y el campo queda en `NULL`.

### AWS Parameter Store

Los perfiles `qa` y `prod` importan secretos automáticamente desde Parameter Store al arrancar:

| Perfil | Namespace                   |
|--------|-----------------------------|
| `qa`   | `/docurural/qa/`            |
| `prod` | `/docurural/prod/`          |

Las claves SSM esperadas en cada namespace son:

| Clave SSM        | Propiedad Spring                      |
|------------------|---------------------------------------|
| `db-password`    | `spring.datasource.password`          |
| `jwt-secret`     | `docurural.security.jwt.secret`       |
| `s3-bucket-docs` | `docurural.storage.s3.bucket`         |
| `aws-region`     | `docurural.storage.s3.region`         |

En `dev` no se importa Parameter Store; los mismos secretos se resuelven desde las variables de entorno del `.env`.

### Credenciales AWS en desarrollo local

```bash
aws configure sso --profile docurural-dev
aws sso login --profile docurural-dev

export AWS_PROFILE=docurural-dev
export AWS_REGION=us-east-1
./mvnw spring-boot:run
```

### Formato de errores

Todos los errores siguen la siguiente estructura:

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

---

## Seguridad

El sistema implementa autenticación **stateless** basada en JWT (HS256):

1. El cliente envía `POST /api/auth/login` con credenciales (`email`, `password`).
2. El servidor valida las credenciales, genera un token JWT firmado y lo devuelve.
3. En cada solicitud posterior, el cliente incluye el token en la cabecera `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` intercepta la solicitud, valida el token y puebla el `SecurityContextHolder`.
5. El control de acceso por rol se realiza con `@PreAuthorize` en los controladores.
6. Las sesiones son **STATELESS**; CSRF está deshabilitado.

### Roles de usuario

| Rol      | Descripción                                                    |
|----------|----------------------------------------------------------------|
| `ADMIN`  | Acceso completo: gestión de usuarios, documentos y categorías. |
| `EDITOR` | Puede cargar y editar solo sus propios documentos.             |
| `READER` | Solo puede consultar y descargar documentos.                   |

Para más detalles, consulta [`docs/arquitectura.md`](docs/arquitectura.md).

---

## Base de datos

Flyway gestiona el versionado del esquema. Las migraciones se encuentran en:

```
src/main/resources/db/migration/
├── V1__init_schema.sql                            # Crea las 4 tablas base del sistema
├── V2__seed_categories.sql                        # Carga las 8 categorías documentales predefinidas
├── V3__add_token_version.sql                      # Agrega token_version a users (revocación de JWT)
├── V4__normalize_email_lowercase.sql              # Normalización de emails e índice funcional
├── V5__add_document_date_index.sql                # Índice en documents.document_date (Sprint 3)
├── V6__add_category_default_sensitivity_level.sql # Sensibilidad por defecto en categorías (HU-28B)
├── V7__add_document_sensitivity_level.sql         # Sensibilidad en documentos (HU-28/HU-29)
└── V8__add_document_file_hash.sql                 # Hash SHA-256 por documento (HU-30)
```

El modo DDL de Hibernate es `validate`: **nunca crea ni modifica tablas automáticamente**.

### Categorías predefinidas

| Categoría       | Descripción                                                             |
|-----------------|-------------------------------------------------------------------------|
| Actas           | Actas de reuniones, consejos directivos, comités                        |
| Resoluciones    | Resoluciones rectorales y administrativas                               |
| Matrículas      | Documentos de inscripción y registro de estudiantes                     |
| Certificados    | Constancias de estudio, certificados de notas, diplomas                 |
| Correspondencia | Comunicados oficiales enviados y recibidos                              |
| Informes        | Informes pedagógicos, académicos, de gestión                            |
| Normatividad    | Manuales de convivencia, PEI, planes de área, protocolos de laboratorio |
| Otro            | Documentos que no corresponden a ninguna categoría anterior             |

Para el diagrama entidad-relación y la descripción detallada de tablas, consulta
[`docs/modelo-datos.md`](docs/modelo-datos.md).

---

## Pruebas

```bash
# Ejecutar todas las pruebas
./mvnw clean test

# Ejecutar una clase de prueba específica
./mvnw test -Dtest=AuthControllerWebMvcTest

# Ejecutar con reporte de cobertura JaCoCo (exige ≥ 80% de líneas)
./mvnw clean verify
```

El reporte HTML de cobertura se genera en:

```
target/site/jacoco/index.html
```

---

## Documentación adicional

| Documento                                      | Descripción                                                          |
|------------------------------------------------|----------------------------------------------------------------------|
| [`docs/arquitectura.md`](docs/arquitectura.md) | Arquitectura del sistema, flujo de seguridad y decisiones de diseño. |
| [`docs/modelo-datos.md`](docs/modelo-datos.md) | Modelo entidad-relación y descripción detallada de tablas.           |
