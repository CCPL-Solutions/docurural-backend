# Arquitectura del sistema — DocuRural Backend

## Tabla de contenidos

- [Visión general](#visión-general)
- [Organización de paquetes](#organización-de-paquetes)
- [Capas de la aplicación](#capas-de-la-aplicación)
- [Flujo de una solicitud HTTP](#flujo-de-una-solicitud-http)
- [Flujo de seguridad JWT](#flujo-de-seguridad-jwt)
- [Manejo de errores](#manejo-de-errores)
- [Auditoría](#auditoría)
- [Internacionalización (i18n)](#internacionalización-i18n)
- [Configuración por perfil](#configuración-por-perfil)

---

## Visión general

DocuRural Backend es una API REST construida con **Spring Boot 3.5** y **Java 17**. La arquitectura sigue los principios
de separación de responsabilidades a través de capas bien definidas dentro de cada módulo funcional.

```
Cliente (Angular / Postman)
        │
        │  HTTP + JWT
        ▼
┌───────────────────────────────────────────┐
│              Spring Boot API              │
│                                           │
│  JwtAuthenticationFilter                 │
│         │                                 │
│         ▼                                 │
│    SecurityContextHolder                  │
│         │                                 │
│         ▼                                 │
│    Controllers  (@PreAuthorize)           │
│         │                                 │
│         ▼                                 │
│      Services  (lógica de negocio)        │
│         │                                 │
│         ▼                                 │
│    Repositories  (Spring Data JPA)        │
│         │                                 │
│         ▼                                 │
│      PostgreSQL  (Flyway migrations)      │
└───────────────────────────────────────────┘
```

---

## Organización de paquetes

Se usa la estrategia **package-by-feature** (paquete por funcionalidad). Cada módulo agrupa todos sus artefactos
relacionados: controller, service, repository, entidades, DTOs y mapper.

```
co.edu.docurural/
├── auth/           Autenticación: login y logout con JWT
├── user/           CRUD de usuarios del sistema
├── document/       Gestión, búsqueda y filtrado de documentos
├── category/       Categorías documentales
├── activitylog/    Log de auditoría de acciones
├── dashboard/      Panel de control — endpoint agregado read-only (DSH-01)
└── shared/         Infraestructura transversal a todos los módulos
    ├── config/     Beans de configuración de Spring
    ├── security/   Filtro JWT, proveedor de tokens, UserDetails
    ├── domain/     Entidad User compartida entre módulos
    ├── exception/  Manejador global de excepciones
    ├── audit/      Contexto de auditoría por solicitud
    ├── dto/        DTOs de uso general (ApiErrorResponse, MessageResponse)
    └── util/       Utilidades transversales (MessageResolver, SortingValidator)
```

#### Módulo dashboard — patrón aggregator

El módulo `dashboard` es una excepción deliberada al patrón estándar: no posee entidad ni repositorio propios.
`DashboardService` es un **aggregator read-only** que ejecuta 3 consultas en una sola transacción sobre
`DocumentRepository` y `CategoryRepository` para componer la respuesta de `GET /api/dashboard/stats` (DSH-01).
Este diseño minimiza los roundtrips al servidor, requisito clave para conexiones lentas del entorno rural.

### Ventajas de package-by-feature

- Alta cohesión: todo lo relacionado con una funcionalidad vive junto.
- Bajo acoplamiento: los módulos no dependen entre sí directamente.
- Navegabilidad: fácil de localizar el código de una funcionalidad.

---

## Capas de la aplicación

Cada módulo de feature sigue las mismas capas internas:

```
Controller  →  Service  →  Repository  →  Entity
    ↕               ↕
   DTO            Mapper
```

| Capa           | Responsabilidad                                                                    |
|----------------|------------------------------------------------------------------------------------|
| **Controller** | Recibir la solicitud HTTP, delegar al servicio, devolver la respuesta.             |
| **Service**    | Lógica de negocio, validación de reglas, orquestación y registro de auditoría.     |
| **Repository** | Acceso a la base de datos mediante Spring Data JPA.                                |
| **Entity**     | Representación de la tabla en la base de datos.                                    |
| **DTO**        | Objetos de transferencia de datos para entrada y salida de la API.                 |
| **Mapper**     | Transformación entre entidades y DTOs (sin usar MapStruct, con métodos estáticos). |

---

## Flujo de una solicitud HTTP

A continuación se describe el flujo completo de una solicitud autenticada:

```
1. Cliente envía: GET /api/users
   Authorization: Bearer <jwt>

2. JwtAuthenticationFilter
   ├── Extrae el token de la cabecera Authorization
   ├── Valida la firma, expiración e issuer con JwtTokenProvider
   ├── Carga el usuario desde BD con CustomUserDetailsService
   └── Pone el Authentication en SecurityContextHolder

3. SecurityConfig
   └── Verifica que la ruta requiere autenticación (todas excepto /auth/login)

4. UserController.list()
   ├── @PreAuthorize("hasRole('ADMIN')") — verifica el rol
   ├── Recibe parámetros sortBy/sortDir
   ├── Resuelve AuditContext (actor + IP) con AuditContextResolver
   └── Delega a UserService.list()

5. UserService.list()
   ├── Valida los parámetros de ordenamiento
   ├── Consulta UserRepository.findAll(Sort)
   └── Mapea con UserMapper.toListResponse()

6. Respuesta: 200 OK + JSON { total, users: [...] }
```

---

## Flujo de seguridad JWT

### Emisión del token (login)

```
POST /api/auth/login
Body: { "email": "...", "password": "..." }
       │
       ▼
AuthService.login()
       │
       ├── AuthenticationManager.authenticate()
       │       └── CustomUserDetailsService.loadUserByUsername()
       │               └── UserRepository.findByEmail()
       │
       ├── Verifica que el usuario esté ACTIVE
       │       (DisabledException si INACTIVE → 403)
       │
       ├── JwtTokenProvider.generateToken(user)
       │       ├── claim "sub" = email
       │       ├── claim "role" = rol del usuario
       │       ├── claim "iss" = JWT_ISSUER
       │       └── expira en JWT_EXPIRATION_MS ms
       │
       ├── Actualiza user.lastLogin
       └── Registra ActivityAction.LOGIN en activity_log

Respuesta 200:
{
  "token": "eyJ...",
  "type": "Bearer",
  "expiresInSeconds": 1800,
  "user": { "id", "fullName", "email", "role" }
}
```

### Validación del token (solicitudes posteriores)

```
JwtAuthenticationFilter (OncePerRequestFilter)
       │
       ├── Extrae "Authorization: Bearer <token>"
       ├── JwtTokenProvider.validateToken(token)
       │       ├── Verifica firma con JWT_SECRET
       │       ├── Verifica expiración
       │       └── Verifica issuer
       │
       ├── Extrae email del claim "sub"
       ├── CustomUserDetailsService.loadUserByUsername(email)
       └── UsernamePasswordAuthenticationToken → SecurityContextHolder
```

### Clases involucradas

| Clase                      | Responsabilidad                                                            |
|----------------------------|----------------------------------------------------------------------------|
| `JwtTokenProvider`         | Generar y validar tokens JWT (Auth0 java-jwt HS256).                       |
| `JwtAuthenticationFilter`  | Interceptar cada request, validar token, poblar contexto.                  |
| `CustomUserDetailsService` | Cargar `UserDetails` desde la base de datos por email.                     |
| `CustomUserPrincipal`      | Implementación de `UserDetails` con datos del `User`.                      |
| `JwtProperties`            | Propiedades configurables: secret, expiration, issuer.                     |
| `SecurityConstants`        | Constantes de cabeceras y prefijos HTTP.                                   |
| `SecurityConfig`           | Configuración de Spring Security: rutas públicas, stateless, entry points. |

---

## Manejo de errores

Todas las excepciones son capturadas por `GlobalExceptionHandler` (`@RestControllerAdvice`) y se traducen al formato
estándar `ApiErrorResponse`:

```json
{
  "timestamp": "2026-05-07T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensaje descriptivo en español",
  "fieldErrors": {
    "campo": "error"
  }
}
```

### Mapa de excepciones a códigos HTTP

| Excepción                                 | HTTP | Descripción                                              |
|-------------------------------------------|------|----------------------------------------------------------|
| `MethodArgumentNotValidException`         | 400  | Fallo de Bean Validation en el cuerpo de la request.     |
| `HttpMessageNotReadableException`         | 400  | JSON mal formado o enum inválido.                        |
| `BusinessRuleException(INVALID_ARGUMENT)` | 400  | Regla de negocio violada (ej: contraseñas no coinciden). |
| `BadCredentialsException`                 | 401  | Email o contraseña incorrectos.                          |
| `AuthenticationException`                 | 401  | Token ausente, expirado o inválido.                      |
| `DisabledException`                       | 403  | Cuenta desactivada (status = INACTIVE).                  |
| `AccessDeniedException`                   | 403  | Rol insuficiente para el recurso.                        |
| `BusinessRuleException(FORBIDDEN)`        | 403  | Acción prohibida (ej: admin se desactiva a sí mismo).    |
| `ResourceNotFoundException`               | 404  | Recurso no encontrado por ID.                            |
| `ConflictException`                       | 409  | Conflicto de unicidad (ej: email duplicado).             |
| `Exception` (genérico)                    | 500  | Error inesperado del servidor.                           |

> Los errores 500 nunca exponen detalles internos al cliente; el mensaje real se registra en los logs del servidor.

---

## Auditoría

Cada acción relevante del sistema queda registrada en la tabla `activity_log` a través de `ActivityLogService`.

### Contexto de auditoría

`AuditContextResolver` construye un `AuditContext` en la capa web (antes del servicio) con:

- `actorUserId`: ID del usuario autenticado que realiza la acción.
- `ipAddress`: IP del cliente, resuelta por `ClientIpResolver` (soporta cabeceras de proxy: `X-Forwarded-For`,
  `X-Real-IP`).

### Acciones auditables

| Acción                | Cuándo se registra                                                                                                                                                                                                                                                                                    |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LOGIN`               | Login exitoso.                                                                                                                                                                                                                                                                                        |
| `LOGOUT`              | Cierre de sesión.                                                                                                                                                                                                                                                                                     |
| `CREATE_USER`         | Creación de un nuevo usuario.                                                                                                                                                                                                                                                                         |
| `EDIT_USER`           | Actualización de datos de un usuario.                                                                                                                                                                                                                                                                 |
| `DEACTIVATE_USER`     | Cambio de estado (activar o desactivar).                                                                                                                                                                                                                                                              |
| `UPLOAD`              | Carga de un documento.                                                                                                                                                                                                                                                                                |
| `DOWNLOAD`            | Descarga de un documento.                                                                                                                                                                                                                                                                             |
| `VIEW`                | Visualización de un documento.                                                                                                                                                                                                                                                                        |
| `EDIT_DOC`            | Edición de metadatos de un documento.                                                                                                                                                                                                                                                                 |
| `DELETE_DOC`          | Eliminación lógica de un documento.                                                                                                                                                                                                                                                                   |
| `CREATE_CATEGORY`     | Creación de una categoría.                                                                                                                                                                                                                                                                            |
| `EDIT_CATEGORY`       | Edición de una categoría.                                                                                                                                                                                                                                                                             |
| `DEACTIVATE_CATEGORY` | Desactivación de una categoría.                                                                                                                                                                                                                                                                       |
| `SEARCH`              | Búsqueda por texto libre (Sprint 3, HU-20/HU-22). **Solo se registra cuando el parámetro `q` está presente**; las consultas de solo filtros (HU-21) no generan registro. `document_id` es siempre `null`. El campo `detail` incluye el término buscado, los filtros activos y el total de resultados. |

---

## Internacionalización (i18n)

Todos los mensajes de error y respuesta dirigidos al usuario se obtienen de `src/main/resources/messages.properties`
mediante `MessageResolver`, un wrapper de `MessageSource`.

### Ventajas

- Los mensajes están centralizados y son fácilmente modificables.
- El `GlobalExceptionHandler` nunca tiene literales de texto en el código.
- Los campos de validación (`@NotBlank`, `@Email`, etc.) de Bean Validation también apuntan a claves en
  `messages.properties`.

### Ejemplo de claves

```properties
auth.login.invalid-credentials=Correo o contraseña incorrectos.
auth.login.account-disabled=Su cuenta ha sido desactivada. Contacte al administrador.
auth.logout.success=Sesión cerrada exitosamente.
user.not-found=Usuario no encontrado con id {0}.
user.email.already-registered=El correo electrónico ya está registrado.
user.created.success=Usuario creado exitosamente.
```

---

## Configuración por perfil

| Archivo                 | Perfil | Descripción                                               |
|-------------------------|--------|-----------------------------------------------------------|
| `application.yaml`      | Todos  | Configuración base: datasource, Flyway, JWT, SpringDoc.   |
| `application-dev.yaml`  | `dev`  | Logs SQL en nivel `DEBUG`, formato de logs legible.       |
| `application-prod.yaml` | `prod` | Logs en nivel `INFO`, stack traces ocultos en respuestas. |

---