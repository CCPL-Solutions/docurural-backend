package co.edu.docurural.category.controller;

import co.edu.docurural.category.dto.CategoryDetailResponseDto;
import co.edu.docurural.category.dto.CategoryListResponseDto;
import co.edu.docurural.category.dto.CreateCategoryRequestDto;
import co.edu.docurural.category.dto.CreateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponseDto;
import co.edu.docurural.category.service.CategoryService;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.dto.ApiErrorResponseDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del módulo de Categorías (CAT-03 / HU-16).
 *
 * <p>Todos los endpoints requieren rol {@code ADMIN}. El {@code context-path}
 * {@code /api} se configura globalmente, por eso el mapping solo incluye
 * {@code /categories}.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categorías", description = "Gestión de categorías documentales (solo ADMIN) — CAT-01..CAT-05")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuditContextResolver auditContextResolver;

    /**
     * CAT-01 / HU-19: listado completo de categorías (200).
     */
    @Operation(
            summary = "Listar categorías",
            description = "Devuelve todas las categorías (ACTIVE e INACTIVE) con conteo de documentos activos y resumen de estados. Solo accesible para el rol ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado retornado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CategoryListResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de ordenamiento inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @GetMapping
    public ResponseEntity<CategoryListResponseDto> list(
            @Parameter(name = "sortBy", description = "Campo de ordenamiento: name | createdAt", example = "name")
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @Parameter(name = "sortDir", description = "Dirección: asc | desc", example = "asc")
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        log.debug("GET /categories sortBy={} sortDir={}", sortBy, sortDir);
        return ResponseEntity.ok(categoryService.list(sortBy, sortDir));
    }

    /**
     * CAT-02 / HU-19: detalle de una categoría por id (200).
     */
    @Operation(
            summary = "Obtener categoría por ID",
            description = "Devuelve el detalle de una categoría con su conteo de documentos activos. Solo accesible para el rol ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos de la categoría retornados exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CategoryDetailResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDetailResponseDto> getById(
            @Parameter(name = "id", description = "ID de la categoría a consultar", example = "1")
            @PathVariable Long id) {
        log.debug("GET /categories/{}", id);
        return ResponseEntity.ok(categoryService.findById(id));
    }

    /**
     * CAT-03 / HU-16: crea una nueva categoría documental (201).
     */
    @Operation(
            summary = "Crear categoría",
            description = "Crea una nueva categoría documental. Solo accesible para el rol ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateCategoryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateCategoryResponseDto> create(
            @Valid @RequestBody CreateCategoryRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("POST /categories name='{}'", request.name());
        CreateCategoryResponseDto response = categoryService.create(request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CAT-05 / HU-18: activa o desactiva una categoría (200).
     */
    @Operation(
            summary = "Activar/Desactivar categoría",
            description = "Cambia el estado de una categoría a ACTIVE o INACTIVE. Solo accesible para el rol ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateCategoryStatusResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido o categoría ya en ese estado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UpdateCategoryStatusResponseDto> changeStatus(
            @Parameter(name = "id", description = "ID de la categoría", example = "9")
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryStatusRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("PATCH /categories/{}/status newStatus={}", id, request.status());
        UpdateCategoryStatusResponseDto response = categoryService.changeStatus(
                id, request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * CAT-04 / HU-17: edita una categoría existente (200).
     */
    @Operation(
            summary = "Actualizar categoría",
            description = "Actualiza el nombre y/o descripción de una categoría existente. Solo accesible para el rol ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateCategoryResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN) o categoría INACTIVE",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UpdateCategoryResponseDto> update(
            @Parameter(name = "id", description = "ID de la categoría a editar", example = "9")
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("PUT /categories/{}", id);
        UpdateCategoryResponseDto response = categoryService.update(id, request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }
}
