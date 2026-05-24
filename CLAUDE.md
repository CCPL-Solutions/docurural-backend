# CLAUDE.md

## Descripción del Proyecto

**Nombre:** DocuRural  
**Dominio:** Gestion documental y archivo digital  
**Proposito:** Sistema web para digitalizar, organizar y consultar documentos institucionales de la IERD Mina y Ticha (
escuela rural en Guacheta, Cundinamarca). Permite a docentes y administrativos cargar, clasificar, buscar y descargar
documentos con control de acceso por roles.  
**Estado:** MVP v1.0 — Sprint completo.

---

## Stack Tecnologico

| Capa                   | Tecnologia                                               |
|------------------------|----------------------------------------------------------|
| Lenguaje               | Java 17                                                  |
| Framework              | Spring Boot 3.5                                          |
| Seguridad              | Spring Security 6 — JWT stateless (Auth0 HS256)          |
| Persistencia           | Spring Data JPA + PostgreSQL                             |
| Migraciones            | Flyway                                                   |
| Mapeo                  | MapStruct 1.5.5.Final                                    |
| Validacion de archivos | Apache Tika 2.9.2                                        |
| Documentacion API      | SpringDoc OpenAPI (Swagger UI en `/api/swagger-ui.html`) |
| Cobertura              | JaCoCo (umbral: 80% de lineas)                           |

---

## Arquitectura

Organizacion **package-by-feature**. Cada modulo sigue las capas internas:
`controller → service (interfaz + impl) → repository → entity / dto / mapper`.

```
co.edu.docurural/
├── activitylog/    # Registro de auditoria de acciones de usuario
├── auth/           # Autenticacion y emision de sesiones JWT
├── category/       # Taxonomia de categorias documentales
├── dashboard/      # Estadisticas y resumen para el panel de control
├── document/       # Ciclo de vida de documentos; aplica CQRS con cinco
│   └── service/    #   servicios especializados: escritura, lectura, busqueda,
│                   #   contenido de archivo y carga por lote
└── shared/
    ├── audit/      # Resolucion del contexto de auditoria (actor e IP del cliente)
    ├── config/     # Configuracion de seguridad, CORS, OpenAPI y datos iniciales
    ├── dto/        # Tipos de respuesta compartidos entre modulos
    ├── exception/  # Manejo centralizado de errores
    ├── security/   # Validacion de tokens JWT y filtros de autenticacion
    └── util/       # Utilidades transversales: i18n, nombres de archivo, ordenacion
├── user/           # Gestion de usuarios y control de acceso por roles
```

---

## Comandos Esenciales

### Build y ejecucion

```bash
./mvnw clean install       # Compilar y empaquetar
./mvnw spring-boot:run     # Levantar la aplicacion
```

### Tests

```bash
./mvnw clean test                              # Ejecutar todas las pruebas
./mvnw test -Dtest=AuthControllerWebMvcTest    # Ejecutar una clase de prueba
./mvnw test -Dtest=AuthControllerWebMvcTest#should_returnToken_when_credentialsAreValid  # Un solo metodo
```

### Calidad de codigo

```bash
./mvnw clean verify   # Ejecutar pruebas + reporte JaCoCo (falla si cobertura < 80%)
```

---

## Configuracion Local

Copia `.env.example` como `.env` y ajusta los valores:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=docurural_db
DB_USER=docurural
DB_PASSWORD=<password>

JWT_SECRET=<cadena aleatoria de 32+ bytes>
JWT_EXPIRATION_MS=1800000
JWT_ISSUER=docurural

SPRING_PROFILES_ACTIVE=dev   # dev: logs SQL + nivel debug | prod: oculta stack traces

CORS_ALLOWED_ORIGINS=http://localhost:4200

# Opcional: crea el primer administrador al iniciar (operacion idempotente)
ADMIN_SEED_EMAIL=admin@example.com
ADMIN_SEED_PASSWORD=password123
```