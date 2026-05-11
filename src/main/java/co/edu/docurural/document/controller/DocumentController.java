package co.edu.docurural.document.controller;

import co.edu.docurural.document.dto.BatchUploadDocumentRequest;
import co.edu.docurural.document.dto.BatchUploadDocumentResponse;
import co.edu.docurural.document.dto.DocumentDetailResponse;
import co.edu.docurural.document.dto.DocumentViewContent;
import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.enums.DocumentFormat;
import co.edu.docurural.document.service.DocumentBatchService;
import co.edu.docurural.document.service.DocumentService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/**
 * Controlador REST del módulo de Documentos (DOC-01..DOC-08).
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

    private final DocumentService documentService;
    private final DocumentBatchService documentBatchService;
    private final AuditContextResolver auditContextResolver;

    private static final EnumSet<DocumentFormat> INLINE_FORMATS =
            EnumSet.of(DocumentFormat.PDF, DocumentFormat.JPG, DocumentFormat.PNG);

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
        return ResponseEntity.ok(documentService.findDetailById(id));
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
            @ApiResponse(responseCode = "200", description = "Stream del archivo retornado exitosamente",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary"))),
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
        DocumentViewContent content = documentService.openForView(id, audit);

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
                .header("X-File-Name", content.originalFileName())
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
        UploadDocumentResponse response = documentService.upload(
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
