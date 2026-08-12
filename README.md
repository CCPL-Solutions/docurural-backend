# DocuRural — Backend

API REST del sistema de gestión documental y archivo digital de la **IERD Miña y Ticha**. Permite administrar
usuarios, categorías y documentos institucionales, con control de acceso por rol, niveles de confidencialidad y
trazabilidad completa de cada acción realizada en el sistema.

> Versión actual: `1.0.0-SNAPSHOT`. Aún no se ha publicado una versión productiva — ver [`CHANGELOG.md`](CHANGELOG.md).

---

## Stack tecnológico

| Componente         | Tecnología                                    |
|--------------------|------------------------------------------------|
| Lenguaje           | Java 17                                         |
| Framework          | Spring Boot 3.5.13                              |
| Seguridad          | Spring Security 6 + JWT HS256 (`com.auth0:java-jwt` 4.4.0) |
| Persistencia       | Spring Data JPA + Hibernate                     |
| Base de datos      | PostgreSQL                                      |
| Migraciones        | Flyway                                          |
| Documentación API  | SpringDoc OpenAPI 2.8.17 (Swagger UI)           |
| Mapeo DTO ↔ entidad| MapStruct 1.5.5.Final                           |
| Validación MIME    | Apache Tika 2.9.2 (magic bytes, no por extensión) |
| Almacenamiento     | AWS S3 (SDK v2), con alternativa local para desarrollo |
| Secretos           | AWS Parameter Store (`spring-cloud-aws` 3.3.1)  |
| Utilidades         | Lombok                                          |
| Pruebas            | JUnit 5, Mockito, AssertJ, Spring Security Test |
| Cobertura          | JaCoCo 0.8.12 (mínimo 80% líneas / 65% ramas)   |
| Build              | Maven Wrapper (`mvnw`)                          |

---

## Requisitos previos

- **JDK 17** o superior
- **PostgreSQL** en ejecución
- **Maven** (o usar el wrapper incluido `./mvnw`)
- **AWS CLI** configurado con un perfil que tenga acceso a S3 y Parameter Store (necesario en `qa` y `prod`; opcional
  en `dev` si se usa el proveedor de almacenamiento `local`)

---

## Instalación y ejecución

```bash
# 1. Clonar el repositorio
git clone https://github.com/CCPL-Solutions/docurural-backend.git
cd docurural-backend

# 2. Configurar variables de entorno (ver docs/configuracion.md para el detalle de cada una)
cp .env.example .env

# 3. Compilar el proyecto
./mvnw clean install

# 4. Ejecutar la aplicación
./mvnw spring-boot:run
```

La aplicación arrancará en `http://localhost:8080/api`. La documentación interactiva de la API (Swagger UI) queda
disponible en `http://localhost:8080/api/swagger-ui.html` — deshabilitada en el perfil `prod`.

---

## Documentación adicional

| Documento                                          | Contenido                                                                              |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------|
| [`docs/configuracion.md`](docs/configuracion.md)       | Variables de entorno, perfiles de Spring y AWS Parameter Store.                              |
| [`docs/arquitectura.md`](docs/arquitectura.md)           | Estructura de paquetes del proyecto (package-by-feature).                                       |
| [`docs/api-rest.md`](docs/api-rest.md)                     | Referencia completa de endpoints, almacenamiento de archivos y formato de errores.                 |
| [`docs/seguridad.md`](docs/seguridad.md)                     | Autenticación JWT, roles, niveles de confidencialidad y CORS.                                        |
| [`docs/modelo-datos.md`](docs/modelo-datos.md)                 | Migraciones, entidades, relaciones, enums de dominio y categorías predefinidas.                        |
| [`docs/pruebas.md`](docs/pruebas.md)                             | Cómo ejecutar las pruebas y generar el reporte de cobertura.                                             |
| [`docs/ci-cd.md`](docs/ci-cd.md)                                   | Flujo de ramas (GitFlow), pipelines de CI/CD y despliegue.                                                  |
| [`CHANGELOG.md`](CHANGELOG.md)                                       | Historial de versiones desplegadas a producción y su contenido.                                                |
