# CLAUDE.md — Convenciones de DocuRural Backend

Convenciones operativas del día a día. Las reglas no negociables (arquitectura, pruebas, base de
datos, clean code mínimo) están en `.specify/memory/constitution.md` y prevalecen sobre este
documento. Aquí vive solo el detalle: naming, formato y patrones concretos.

## Comandos habituales

```bash
./mvnw clean verify          # compila, prueba y valida umbrales JaCoCo (puerta de calidad)
./mvnw test                  # solo pruebas
./mvnw spring-boot:run       # arranca en local (perfil local)
```

El perfil `test` queda fijado por Surefire, así que `verify` no intenta leer AWS Parameter Store
aunque tengas `SPRING_PROFILES_ACTIVE` exportado en la shell.

## Naming de clases Java

| Elemento              | Patrón                         | Ejemplo                        |
|-----------------------|--------------------------------|--------------------------------|
| Entidad JPA           | `PascalCase` sin sufijo        | `Document`, `Category`         |
| Interfaz de servicio  | `{Dominio}Service`             | `DocumentCommandService`       |
| Implementación        | `{Dominio}ServiceImpl`         | `DocumentCommandServiceImpl`   |
| Controlador REST      | `{Dominio}Controller`          | `DocumentController`           |
| Repositorio           | `{Dominio}Repository`          | `DocumentRepository`           |
| Mapper                | `{Dominio}Mapper`              | `DocumentMapper`               |
| DTO de entrada        | `{Accion}{Dominio}RequestDto`  | `UploadDocumentRequestDto`     |
| DTO de salida         | `{Accion}{Dominio}ResponseDto` | `UploadDocumentResponseDto`    |
| DTO en general        | `{Concepto}Dto`                | `DocumentSummaryDto`           |
| Enum                  | `{Dominio}{Concepto}`          | `DocumentStatus`, `UserRole`   |
| Excepción de dominio  | `{Concepto}Exception`          | `BusinessRuleException`        |
| Proyección JPA        | `{Dominio}{Datos}View`         | `CategoryNameView`             |
| Validador Bean        | `{Regla}Validator`             | `PasswordsMatchValidator`      |
| Clase de prueba       | `{ClaseTesteada}Test`          | `DocumentCommandServiceTest`   |
| Prueba de controlador | `{Controlador}WebMvcTest`      | `DocumentControllerWebMvcTest` |

## Prefijos de métodos de servicio

| Operación       | Prefijo              | Ejemplo                            |
|-----------------|----------------------|------------------------------------|
| Crear / cargar  | `create` / `upload`  | `create(request, audit)`, `upload` |
| Leer por ID     | `findById`           | `findById(Long id)`                |
| Leer lista      | `list` / `search`    | `list(sortBy, sortDir)`, `search` |
| Actualizar      | `update`             | `updateMetadata`                   |
| Eliminar (soft) | `delete`             | `deleteLogical`                    |
| Servir archivo  | `openFor{Uso}`       | `openForView`, `openForDownload`   |
| Predicado       | `is` / `requires`    | `isActiveApprover(userId)`         |

## Naming de base de datos

| Elemento  | Patrón                    | Ejemplo                        |
|-----------|---------------------------|--------------------------------|
| Tabla     | `snake_case` plural       | `activity_log`, `documents`    |
| Columna   | `snake_case`              | `document_date`, `uploaded_by` |
| Índice    | `idx_{tabla}_{col}`       | `idx_documents_status`         |
| Migración | `V{n}__{descripcion}.sql` | `V3__add_can_approve_to_users.sql` |

## Paquetes

`snake_case` singular, agrupados por feature: `auth`, `user`, `document`, `category`,
`activitylog`, `dashboard`, `health`, `shared`.

## Claves i18n (`messages.properties`)

Formato `{modulo}.{entidad}.{concepto}`, en minúsculas y con guiones:
`document.file.too-large`, `user.not-found`.

## Nombres al estilo clean code

- Los nombres revelan intención: `calculateTotalWithDiscount()`, no `calc()` ni `doStuff()`.
- Sin encodings ni prefijos húngaros: no `IDocumentService`, no `strName`, no `objDocument`.
- Clases: sustantivos (`DocumentAccessValidator`). Métodos: verbos (`validateAccess`).
- Booleanos con prefijo `is`, `has`, `can`: `isActive`, `hasContent`, `canBeDeleted`.
- Sin abreviaturas salvo las ya establecidas en el proyecto: `dto`, `id`, `ip`.
- Preferir composición sobre herencia.
- Si una clase necesita más de 2-3 colaboradores, probablemente hace demasiado.
- Si un método no entra en pantalla sin scroll, probablemente hace demasiado.
- DRY: si copias un bloque por segunda vez, extráelo. En tests, la duplicación es aceptable
  cuando mejora la legibilidad de cada test por separado.

## Logging

SLF4J vía `@Slf4j` de Lombok. Un log por operación de negocio exitosa.

| Nivel   | Cuándo                                                                            |
|---------|-----------------------------------------------------------------------------------|
| `error` | Excepción capturada o estado inconsistente. Incluir contexto (ej. ID del recurso). |
| `warn`  | Situación inesperada pero recuperable (ej. archivo ausente al limpiar rollback).   |
| `info`  | Evento de negocio relevante (documento cargado, usuario creado).                   |
| `debug` | Flujo interno útil para diagnóstico; activo solo en perfiles `local` y `develop`.   |

```java
log.info("Uploading document title='{}' by userId={}", request.title(), audit.actorUserId());
log.debug("File stored at path={}", filePath);
```

## Patrón MapStruct

Los mappers son **clases abstractas** con `@Mapper(componentModel = "spring")`.

```java
@Mapper(componentModel = "spring")
public abstract class DocumentMapper {

    @BeforeMapping
    protected void requireNonNull(Document doc) {
        Objects.requireNonNull(doc, "document no puede ser null");
    }

    @Mapping(target = "status",
            expression = "java(doc.getStatus() != null ? doc.getStatus().name() : null)")
    public abstract DocumentSummaryResponseDto toSummary(Document doc);
}
```

- `@BeforeMapping` para validaciones de null-safety.
- Expresiones Java para conversiones enum → String.
- Métodos concretos para agregaciones manuales (listas, conteos).

## Módulo `document` — reparto CQRS

| Interfaz                 | Responsabilidad                                                                                          |
|--------------------------|----------------------------------------------------------------------------------------------------------|
| `DocumentCommandService` | Escritura: upload, updateMetadata, deleteLogical, uploadSingleForBatch, raiseSensitivityForCategory      |
| `DocumentQueryService`   | Lectura por ID (`findDetailById`) y conteos de documentos activos por categoría                          |
| `DocumentSearchService`  | Búsqueda con JPA Specification + filtros paginados; opciones del panel de filtros                        |
| `DocumentContentService` | Servicio de archivo: `openForView` / `openForDownload` con `Content-Disposition`                         |
| `DocumentBatchService`   | Carga por lote (hasta 5 archivos)                                                                        |

`DocumentHashService` (cálculo del SHA-256 de integridad) es un servicio de apoyo interno, fuera del reparto
CQRS.

## Módulo `category` — lectura para otros módulos

| Interfaz               | Responsabilidad                                                                                  |
|------------------------|--------------------------------------------------------------------------------------------------|
| `CategoryService`      | Casos de uso de categorías (listar, detalle, crear, editar, cambiar estado). Depende de `document`. |
| `CategoryQueryService` | Consultas de solo lectura para otros módulos (`requiresApproval`). **No** depende de `document`.  |

Un módulo que necesite datos de categorías (p. ej. `document`) inyecta `CategoryQueryService`, no
`CategoryService`: este último ya depende de `document` y cerraría un ciclo (desviación D-4 de la constitución).

## Detalles de pruebas

**Controladores** — plantilla real en uso:

```java
@WebMvcTest(
        controllers = DocumentController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ContentDispositionResolver.class})
class DocumentControllerWebMvcTest { }
```

**Servicios** — `@Spy` con la instancia real del mapper para ejercitar el mapeo de verdad:

```java
@ExtendWith(MockitoExtension.class)
class DocumentCommandServiceTest {
    @Mock DocumentRepository documentRepository;
    @Spy DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);
    @InjectMocks DocumentCommandServiceImpl service;
}
```

**Mappers** — instanciar directamente con `Mappers.getMapper(XyzMapper.class)`. Verificar que los
campos sensibles no se expongan (ej. que `passwordHash` esté ausente en `UserResponseDto`).

**Constantes** — declarar constantes de clase para los datos repetidos:

```java
private static final Long ACTOR_ID = 10L;
private static final AuditContext AUDIT = new AuditContext(ACTOR_ID, "127.0.0.1");
```

**Nombres** — `<accion>_<contexto>_<resultado>`:

```java
void uploadBatch_returnsAllSuccessful_whenAllFilesValid()
void search_returns400_whenQHasOneChar()
void create_persistsAndLogs_whenNameIsUnique()
```

**Fixtures** — usar siempre los builders de `src/test/java/co/edu/docurural/support/TestFixtures.java`
(`userAdmin(id)`, `userEditor(id)`, `userReader(id)`, `userApprover(id)`, `categoryActive(id, name)`,
`categoryRequiringApproval(id, name)`, `uploadDocumentRequest(categoryId)`).

**JaCoCo** — excluidas del umbral (configurado en `pom.xml`): `**/dto/**`, `**/entity/**`,
`**/enums/**`, `**/repository/projection/**`, `DocururalBackendApplication`.
