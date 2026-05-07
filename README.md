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
| Build             | Maven Wrapper (`mvnw`)                 |

---

## Requisitos previos

- **JDK 17** o superior
- **PostgreSQL 15+** en ejecución
- **Maven** (o usar el wrapper incluido `./mvnw`)

---

## Configuración del entorno

Copia el archivo de ejemplo y completa las variables:

```bash
cp .env.example .env
```

| Variable                 | Descripción                                             | Valor por defecto       |
|--------------------------|---------------------------------------------------------|-------------------------|
| `DB_HOST`                | Host de PostgreSQL                                      | `localhost`             |
| `DB_PORT`                | Puerto de PostgreSQL                                    | `5432`                  |
| `DB_NAME`                | Nombre de la base de datos                              | `docurural_db`          |
| `DB_USER`                | Usuario de la base de datos                             | `docurural`             |
| `DB_PASSWORD`            | Contraseña del usuario de BD                            | —                       |
| `JWT_SECRET`             | Clave secreta para firmar tokens JWT (mínimo 32 bytes)  | —                       |
| `JWT_EXPIRATION_MS`      | Tiempo de vida del token en milisegundos                | `1800000` (30 min)      |
| `JWT_ISSUER`             | Emisor incluido en el claim `iss` del JWT               | `docurural`             |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`dev` o `prod`)                          | `dev`                   |
| `CORS_ALLOWED_ORIGINS`   | Orígenes permitidos en CORS                             | `http://localhost:4200` |
| `ADMIN_SEED_EMAIL`       | Email del administrador inicial (opcional, idempotente) | —                       |
| `ADMIN_SEED_PASSWORD`    | Contraseña del administrador inicial (opcional)         | —                       |

### Perfiles de Spring

- **`dev`**: habilita logs SQL y salida en nivel `DEBUG`. Ideal para desarrollo local.
- **`prod`**: oculta stack traces en las respuestas de error. Recomendado para producción.

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
├── document/              # Metadatos de documentos (Sprint 2)
│   ├── entity/            → Document
│   ├── enums/             → DocumentFormat, DocumentStatus
│   └── repository/        → DocumentRepository
│
├── category/              # Categorías documentales (CAT-03 / HU-16)
│   ├── controller/        → CategoryController
│   ├── dto/               → CreateCategoryRequest, CreateCategoryResponse
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

| Método  | Ruta                 | HU     | Descripción                                        |
|---------|----------------------|--------|----------------------------------------------------|
| `POST`  | `/categories`        | HU-16  | Crea una nueva categoría documental.               |
| `PUT`   | `/categories/{id}`   | HU-17  | Edita el nombre y descripción de una categoría.    |

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
| `EDITOR` | Puede cargar, editar y eliminar documentos.                    |
| `READER` | Solo puede consultar y descargar documentos.                   |

Para más detalles, consulta [`docs/arquitectura.md`](docs/arquitectura.md).

---

## Base de datos

Flyway gestiona el versionado del esquema. Las migraciones se encuentran en:

```
src/main/resources/db/migration/
├── V1__init_schema.sql       # Crea las 4 tablas base del sistema
└── V2__seed_categories.sql   # Carga las 8 categorías documentales predefinidas
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

Para el diagrama entidad-relación y la descripción detallada de tablas, consulta [
`docs/modelo-datos.md`](docs/modelo-datos.md).

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