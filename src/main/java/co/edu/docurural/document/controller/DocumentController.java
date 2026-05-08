package co.edu.docurural.document.controller;

import co.edu.docurural.document.dto.UploadDocumentRequest;
import co.edu.docurural.document.dto.UploadDocumentResponse;
import co.edu.docurural.document.service.DocumentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST del módulo de Documentos (DOC-03 / HU-09).
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
    private final AuditContextResolver auditContextResolver;

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
}
