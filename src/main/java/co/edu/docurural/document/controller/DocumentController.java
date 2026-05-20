package co.edu.docurural.document.controller;

import co.edu.docurural.document.dto.BatchUploadDocumentRequest;
import co.edu.docurural.document.dto.BatchUploadDocumentResponse;
import co.edu.docurural.document.dto.DeleteDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentFileContent;
import co.edu.docurural.document.dto.DocumentListResponse;
import co.edu.docurural.document.dto.UpdateDocumentMetadataRequest;
import co.edu.docurural.document.dto.UpdateDocumentMetadataResponse;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.service.DocumentBatchService;
import co.edu.docurural.document.service.DocumentCommandService;
import co.edu.docurural.document.service.DocumentContentService;
import co.edu.docurural.document.service.DocumentQueryService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/**
 * Controlador REST del módulo de Documentos (DOC-01..DOC-08 / HU-09..HU-14).
 *
 * <p>El {@code context-path} {@code /api} se configura globalmente; el mapping incluye {@code /documents}.
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documentos", description = "Gestión de documentos del repositorio — DOC-01..DOC-08")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;
    private final DocumentContentService documentContentService;
    private final DocumentBatchService documentBatchService;
    private final AuditContextResolver auditContextResolver;

    private static final EnumSet<DocumentFormat> INLINE_FORMATS =
            EnumSet.of(DocumentFormat.PDF, DocumentFormat.JPG, DocumentFormat.PNG);

    /**
     * DOC-01 / HU-15: lista paginada de documentos activos con ordenamiento configurable.
     */
    @Operation(
            summary = "Listar documentos activos",
            description = "DOC-01 — HU-15. Devuelve los documentos con status=ACTIVE paginados y ordenados. "
                    + "Accesible para todos los roles autenticados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de documentos retornada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DocumentListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación u ordenamiento inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'READER')")
    @GetMapping
    public ResponseEntity<DocumentListResponse> list(
            @Parameter(name = "page", description = "Número de página (default 1)", example = "1")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(name = "size", description = "Documentos por página (default 20, máx 50)", example = "20")
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(name = "sortBy", description = "Campo de ordenamiento: createdAt | title | documentDate", example = "createdAt")
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @Parameter(name = "sortDir", description = "Dirección: asc | desc", example = "desc")
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        log.debug("GET /documents page={} size={} sortBy={} sortDir={}", page, size, sortBy, sortDir);
        return ResponseEntity.ok(documentQueryService.list(page, size, sortBy, sortDir));
    }

    /**
     * DOC-02 / HU-11: retorna la ficha completa de metadatos de un documento activo.
     */
    @Operation(
            summary = "Obtener documento por ID",
            description = "DOC-02 — Devuelve los metadatos completos del documento. Accesible para todos los roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ficha del documento retornada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DocumentDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento no existe o fue eliminado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'READER')")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailResponse> getById(@PathVariable Long id) {
        log.debug("GET /documents/{}", id);
        return ResponseEntity.ok(documentQueryService.findDetailById(id));
    }

    /**
     * DOC-05 / HU-13: edita metadatos de un documento activo (200).
     */
    @Operation(
            summary = "Editar metadatos de documento",
            description = "DOC-05 — Actualiza título, descripción, categoría, área responsable y fecha del documento. "
                    + "ADMIN puede editar cualquier documento; EDITOR solo documentos propios.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadatos actualizados exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateDocumentMetadataResponse.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos para editar el documento solicitado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento no existe, fue eliminado o categoría inválida",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<UpdateDocumentMetadataResponse> updateMetadata(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentMetadataRequest request,
            HttpServletRequest httpRequest) {
        log.debug("PUT /documents/{}", id);
        UpdateDocumentMetadataResponse response = documentCommandService.updateMetadata(
                id, request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * DOC-06 / HU-14: elimina lógicamente un documento activo (200).
     */
    @Operation(
            summary = "Eliminar documento (lógico)",
            description = "DOC-06 — Cambia el estado del documento de ACTIVE a DELETED. "
                    + "No elimina el archivo físico en el MVP. Solo accesible para ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento eliminado lógicamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeleteDocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un documento activo con el ID indicado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteDocumentResponse> deleteLogical(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        log.debug("DELETE /documents/{}", id);
        DeleteDocumentResponse response = documentCommandService.deleteLogical(
                id, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * DOC-07 / HU-11: retorna el stream binario del archivo para visualización en línea.
     *
     * <p>PDF, JPG y PNG se sirven con {@code Content-Disposition: inline} para abrir en el navegador.
     * DOCX y XLSX se sirven con {@code Content-Disposition: attachment} para forzar la descarga.
     * El evento {@code VIEW} queda registrado en {@code activity_log}.
     */
    @Operation(
            summary = "Visualizar documento",
            description = "DOC-07 — Devuelve el stream binario del archivo. PDF/JPG/PNG abren en el navegador (inline); "
                    + "DOCX/XLSX se descargan (attachment). Registra acción VIEW en activity_log.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stream del archivo retornado exitosamente; el Content-Type varía según el formato del documento",
                    content = {
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    schema = @Schema(type = "string", format = "binary"))
                    }),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento no existe o archivo físico no disponible",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'READER')")
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.debug("GET /documents/{}/view", id);
        AuditContext audit = auditContextResolver.resolve(httpRequest);
        DocumentFileContent content = documentContentService.openForView(id, audit);

        MediaType mediaType = mediaTypeFor(content.format());
        ContentDisposition disposition = INLINE_FORMATS.contains(content.format())
                ? ContentDisposition.inline()
                  .filename(content.originalFileName(), StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment()
                  .filename(content.originalFileName(), StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.fileSizeBytes())
                .header("X-File-Name", sanitizeForHeader(content.originalFileName()))
                .header("X-File-Size", String.valueOf(content.fileSizeBytes()))
                .body(content.resource());
    }

    private static MediaType mediaTypeFor(DocumentFormat format) {
        return switch (format) {
            case PDF -> MediaType.APPLICATION_PDF;
            case JPG -> MediaType.IMAGE_JPEG;
            case PNG -> MediaType.IMAGE_PNG;
            case DOCX -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case XLSX -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
    }

    private static String sanitizeForHeader(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String sanitized = filename
                .replaceAll("\\p{Cntrl}", "")
                .replace("/", "")
                .replace("\\", "")
                .trim();
        if (sanitized.isEmpty()) {
            return "file";
        }
        return sanitized.length() <= 255 ? sanitized : sanitized.substring(0, 255);
    }

    /**
     * DOC-08 / HU-12: retorna el stream binario del archivo para descarga.
     *
     * <p>Siempre se sirve con {@code Content-Disposition: attachment}, independientemente del formato,
     * para forzar la descarga en el navegador.
     * El evento {@code DOWNLOAD} queda registrado en {@code activity_log}.
     */
    @Operation(
            summary = "Descargar documento",
            description = "DOC-08 — Devuelve el stream binario del archivo con Content-Disposition: attachment "
                    + "para forzar la descarga en el navegador. Accesible para todos los roles. "
                    + "Registra acción DOWNLOAD en activity_log.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stream del archivo retornado para descarga; el Content-Type varía según el formato del documento",
                    content = {
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    schema = @Schema(type = "string", format = "binary"))
                    }),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento no existe o archivo físico no disponible",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'READER')")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, HttpServletRequest httpRequest) {
        log.debug("GET /documents/{}/download", id);
        AuditContext audit = auditContextResolver.resolve(httpRequest);
        DocumentFileContent content = documentContentService.openForDownload(id, audit);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(content.originalFileName(), StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .contentType(mediaTypeFor(content.format()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.fileSizeBytes())
                .header("X-File-Name", sanitizeForHeader(content.originalFileName()))
                .header("X-File-Size", String.valueOf(content.fileSizeBytes()))
                .body(content.resource());
    }

    /**
     * DOC-03 / HU-09: carga un documento individual (201).
     */
    @Operation(
            summary = "Cargar documento",
            description = "Carga un documento al repositorio con sus metadatos. Solo accesible para roles ADMIN y EDITOR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Documento cargado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UploadDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (rol READER)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no existe o está inactiva",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "El archivo supera el tamaño máximo de 10 MB",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "Tipo de archivo no permitido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadDocumentResponse> upload(
            @Valid @ModelAttribute UploadDocumentRequest request,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        log.debug("POST /documents title='{}' categoryId={}", request.title(), request.categoryId());
        UploadDocumentResponse response = documentCommandService.upload(
                request, file, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DOC-04 / HU-10: carga hasta 5 documentos en un lote.
     *
     * <p>Si el request pasa las validaciones y la categoría es válida, la respuesta retorna HTTP 200.
     * En ese caso, cada archivo se procesa de forma independiente: si un archivo falla, los demás se
     * persisten normalmente y los errores individuales se reportan en {@code results[].errorMessage}.
     * Si el request no pasa validaciones o la categoría es inválida, se retorna el
     * {@code ApiErrorResponse} correspondiente.
     */
    @Operation(
            summary = "Cargar documentos (lote)",
            description = "Carga hasta 5 documentos simultáneamente con metadatos comunes. "
                    + "Si un archivo falla (tamaño, formato), los demás se persisten. "
                    + "Solo accesible para roles ADMIN y EDITOR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lote procesado. Revisar results[] para el estado individual de cada archivo.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BatchUploadDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Más de 5 archivos, lote vacío, títulos inválidos o campos comunes faltantes",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (rol READER)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La categoría indicada no existe o está inactiva",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "El tamaño total del lote supera el límite del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadDocumentResponse> uploadBatch(
            @Valid @ModelAttribute BatchUploadDocumentRequest request,
            @RequestPart("files") MultipartFile[] files,
            HttpServletRequest httpRequest) {
        log.debug("POST /documents/batch totalFiles={} categoryId={}",
                files != null ? files.length : 0, request.categoryId());
        BatchUploadDocumentResponse response = documentBatchService.uploadBatch(
                request, files, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }
}
