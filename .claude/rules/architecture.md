# Patrones de Arquitectura y Código

## Convenciones de nombres

### Clases Java

| Elemento              | Patrón                         | Ejemplo                        |
|-----------------------|--------------------------------|--------------------------------|
| Entidad JPA           | `PascalCase` sin sufijo        | `Document`, `Category`         |
| Interfaz de servicio  | `{Dominio}Service`             | `DocumentCommandService`       |
| Implementacion        | `{Dominio}ServiceImpl`         | `DocumentCommandServiceImpl`   |
| Controlador REST      | `{Dominio}Controller`          | `DocumentController`           |
| Repositorio           | `{Dominio}Repository`          | `DocumentRepository`           |
| Mapper                | `{Dominio}Mapper`              | `DocumentMapper`               |
| DTO de entrada        | `{Accion}{Dominio}RequestDto`  | `UploadDocumentRequest`        |
| DTO de salida         | `{Accion}{Dominio}ResponseDto` | `UploadDocumentResponse`       |
| DTOs en general       | `{Concepto}Dto`                | `DocumentSummaryDto`           |
| Enum                  | `{Dominio}{Concepto}`          | `DocumentStatus`, `UserRole`   |
| Excepcion de dominio  | `{Concepto}Exception`          | `BusinessRuleException`        |
| Proyeccion JPA        | `{Dominio}{Datos}View`         | `CategoryNameView`             |
| Validador Bean        | `{Regla}Validator`             | `PasswordsMatchValidator`      |
| Clase de prueba       | `{ClaseTesteada}Test`          | `DocumentCommandServiceTest`   |
| Prueba de controlador | `{Controlador}WebMvcTest`      | `DocumentControllerWebMvcTest` |

### Métodos de servicio

| Operación       | Prefijo              | Ejemplo               |
|-----------------|----------------------|-----------------------|
| Crear / cargar  | `create` / `upload`  | `uploadDocument`      |
| Leer por ID     | `findById`           | `findById(Long id)`   |
| Leer lista      | `findAll` / `search` | `search(filters)`     |
| Actualizar      | `update`             | `updateMetadata`      |
| Eliminar (soft) | `delete`             | `deleteLogical`       |
| Servir archivo  | `getContent`         | `getContent(Long id)` |

### Base de datos

| Elemento  | Patrón                    | Ejemplo                        |
|-----------|---------------------------|--------------------------------|
| Tabla     | `snake_case` plural       | `activity_log`, `documents`    |
| Columna   | `snake_case`              | `document_date`, `uploaded_by` |
| Índice    | `idx_{tabla}_{col}`       | `idx_documents_status`         |
| Migración | `V{n}__{descripcion}.sql` | `V3__add_token_version.sql`    |

### Paquetes

`snake_case` singular, agrupados por feature: `auth`, `user`, `document`, `category`, `activitylog`, `dashboard`,
`shared`.

### Claves i18n (`messages.properties`)

`{modulo}.{entidad}.{concepto}` en minúsculas con guiones: `document.file.too-large`, `user.not-found`.

---

## Inyección de dependencias

Siempre por constructor usando `@RequiredArgsConstructor` de Lombok. Nunca `@Autowired` en campos ni en setters.

```java

@Service
@RequiredArgsConstructor
public class DocumentCommandServiceImpl implements DocumentCommandService {
    private final DocumentRepository documentRepository;
    private final ActivityLogService activityLogService;
}
```

## DTOs

Usar `record` de Java para todos los DTOs de request/response. Son inmutables por diseño.

```java
public record UploadDocumentRequest(
        @NotBlank String title,
        @NotNull Long categoryId
) {
}
```

## Logging

Usar SLF4J con `@Slf4j` de Lombok. Nunca `System.out.println`.

Estándar por nivel:

- `log.error(msg, ex)` — excepción capturada o estado inconsistente; incluir contexto del error (ej. ID del recurso).
- `log.warn(msg)` — situación inesperada pero recuperable (ej. archivo no encontrado en limpieza de rollback).
- `log.info(msg)` — eventos de negocio relevantes (ej. documento cargado, usuario creado). Un log por operación exitosa.
- `log.debug(msg)` — flujo interno útil para diagnóstico; activo solo en perfil `dev`.

```java

@Slf4j
@Service
public class DocumentCommandServiceImpl {
    public UploadDocumentResponse upload(...) {
        log.info("Uploading document title='{}' by userId={}", request.title(), audit.actorUserId());
        // ...
        log.debug("File stored at path={}", filePath);
    }
}
```

## Interfaces de servicio (DIP)

Todo servicio nuevo debe tener interfaz + implementación separada. La interfaz define el contrato; la implementación
lleva `@Service` y el sufijo `Impl`.

## Módulo document — CQRS (5 sub-servicios)

| Interfaz                 | Responsabilidad                                                        |
|--------------------------|------------------------------------------------------------------------|
| `DocumentCommandService` | Escritura: upload, updateMetadata, deleteLogical, uploadSingleForBatch |
| `DocumentQueryService`   | Lectura por ID                                                         |
| `DocumentSearchService`  | Búsqueda con JPA Specification + filtros paginados                     |
| `DocumentContentService` | Servicio de archivo: view/download con Content-Disposition             |
| `DocumentBatchService`   | Carga por lote (hasta 5 archivos)                                      |

Nunca llamar al repositorio de `document` desde otro módulo. Usar las interfaces de servicio.

## Patrón Mapper (MapStruct)

Los mappers son **clases abstractas** (no interfaces) con `@Mapper(componentModel = "spring")`.

```java

@Mapper(componentModel = "spring")
public abstract class DocumentMapper {

    @BeforeMapping
    protected void requireNonNull(Document doc) {
        Objects.requireNonNull(doc, "document no puede ser null");
    }

    @Mapping(target = "status",
            expression = "java(doc.getStatus() != null ? doc.getStatus().name() : null)")
    public abstract DocumentSummaryResponse toSummary(Document doc);
}
```

Convenciones:

- `@BeforeMapping` para validaciones null-safety.
- Expresiones Java para conversiones enum → String.
- Métodos concretos para agregaciones manuales (listas, conteos).

## Auditoría (REQUIRES_NEW)

`ActivityLogService` siempre usa `@Transactional(propagation = Propagation.REQUIRES_NEW)`.

- Los fallos de auditoría **nunca** deben revertir la operación de negocio.
- La implementación captura su propia excepción, registra el error (`log.error`) y continúa.
- No cambiar esta propagación sin aprobación explícita.

## AuditContext

Todo método de servicio que mute estado recibe `AuditContext` (actorUserId + clientIp) como **parámetro explícito** (
generalmente el último).

```java
UploadDocumentResponse upload(UploadDocumentRequest request, MultipartFile file, AuditContext audit);
```

## Errores de negocio

Usar `BusinessRuleException` con `BusinessErrorCode` enum para errores de dominio.

```java
throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN, message);
```

Para añadir un nuevo error: extender `BusinessErrorCode` con su `HttpStatus` asociado. No modificar
`GlobalExceptionHandler`.

Excepciones de infraestructura: `ResourceNotFoundException` (404), `ConflictException` (409), `FileStorageException` (
500).

## Mensajes i18n

Nunca strings hardcodeados en código de negocio. Usar `MessageResolver` con claves de
`src/main/resources/messages.properties`.

```java
String msg = messageResolver.get("document.not-found");
```
