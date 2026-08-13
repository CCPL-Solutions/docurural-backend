[← Volver al README](../README.md)

# Seguridad

El sistema implementa autenticación **stateless** basada en JWT (HS256, `com.auth0:java-jwt`):

1. El cliente envía `POST /api/auth/login` con credenciales (`email`, `password`).
2. El servidor valida las credenciales, genera un token JWT firmado y lo devuelve.
3. En cada solicitud posterior, el cliente incluye el token en la cabecera `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` intercepta la solicitud, valida el token y puebla el `SecurityContextHolder`.
5. El control de acceso por rol se realiza con `@PreAuthorize` en los controladores.
6. Las sesiones son **STATELESS**; CSRF está deshabilitado.

El token expira a los **30 minutos** (`JWT_EXPIRATION_MS`, configurable) y **no existe mecanismo de refresh token**:
al expirar, el cliente debe volver a autenticarse. `POST /auth/logout` es informativo — registra la acción en la
bitácora, pero no invalida el token en el servidor (al ser stateless, el cliente simplemente lo descarta).

## Revocación de tokens por `tokenVersion`

Cada token incluye el claim `tokver` con el valor de `User.tokenVersion` en el momento de la emisión. En cada
solicitud, `JwtAuthenticationFilter` compara ese claim contra el valor actual del usuario en base de datos; si
difieren, la solicitud se rechaza con 401 (`CredentialsExpiredException`). Esto permite invalidar de golpe todos los
tokens vivos de un usuario (por ejemplo, al desactivar su cuenta) incrementando `User.tokenVersion`, sin necesidad de
una lista de revocación.

## Roles de usuario

| Rol      | Descripción                                                       |
|----------|-----------------------------------------------------------------------|
| `ADMIN`  | Acceso completo: gestión de usuarios, documentos y categorías.            |
| `EDITOR` | Puede cargar y editar solo sus propios documentos.                           |
| `READER` | Solo puede consultar y descargar documentos.                                    |

## Niveles de confidencialidad

Cada documento tiene un `SensitivityLevel`, con orden jerárquico:

```
INTERNAL < RESTRICTED < CONFIDENTIAL
```

- Cada categoría define un `defaultSensitivityLevel`; el nivel asignado a un documento nunca puede ser inferior al de
  su categoría. `Matrículas` y `Certificados` nacen como `RESTRICTED` (Ley 1581 de 2012, protección de datos
  personales).
- `EDITOR` no puede asignar niveles `RESTRICTED` ni `CONFIDENTIAL` al cargar o editar un documento.
- `EDITOR` y `READER` reciben `403 Forbidden` al intentar acceder a un documento `RESTRICTED` o `CONFIDENTIAL`; el
  intento queda registrado en la bitácora como la acción `ACCESS_DENIED`.

## Rutas públicas

Las siguientes rutas no requieren autenticación (evaluadas sin el context-path `/api`):

```
POST /auth/login
GET  /version
GET  /actuator/health
/v3/api-docs/**
/swagger-ui/**, /swagger-ui.html, /swagger-resources/**, /webjars/**
```

El resto de rutas exige un token JWT válido (`anyRequest().authenticated()`).

## CORS

Métodos permitidos `GET, POST, PUT, PATCH, DELETE, OPTIONS`; headers permitidos `Authorization`, `Content-Type`,
`X-Requested-With`; se expone `Authorization` para que el frontend pueda leerlo; `allowCredentials: true`; orígenes
configurables vía `CORS_ALLOWED_ORIGINS` (por defecto, el dev server de Angular en `http://localhost:4200`).
