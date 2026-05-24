package co.edu.docurural.user.controller;

import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.user.dto.CreateUserResponseDto;
import co.edu.docurural.user.dto.UpdateStatusResponseDto;
import co.edu.docurural.user.service.UserService;
import co.edu.docurural.shared.dto.ApiErrorResponseDto;
import co.edu.docurural.user.dto.CreateUserRequestDto;
import co.edu.docurural.user.dto.UpdateStatusRequestDto;
import co.edu.docurural.user.dto.UpdateUserRequestDto;
import co.edu.docurural.user.dto.UpdateUserResponseDto;
import co.edu.docurural.user.dto.UserListResponseDto;
import co.edu.docurural.user.dto.UserResponseDto;
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
 * Controlador REST del módulo de Usuarios (USR-01..USR-05).
 *
 * <p>Todos los endpoints requieren rol {@code ADMIN}; la restricción se declara
 * a nivel de clase con {@code @PreAuthorize("hasRole('ADMIN')")} para evitar
 * duplicación. El {@code context-path} {@code /api} se configura globalmente en
 * {@code application.yaml}, por eso aquí el mapping solo incluye {@code /users}.
 *
 * <p>La lógica de negocio, validación de reglas (email único, auto-protección
 * de admin, cambio de estado, etc.) y el registro en {@code activity_log} vive
 * en {@link UserService}. El contexto de auditoría (actor + IP) se resuelve
 * en capa web vía {@link AuditContextResolver}.
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "CRUD de usuarios (solo ADMIN) — USR-01..USR-05")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuditContextResolver auditContextResolver;

    /**
     * USR-01: listado de usuarios ordenado por {@code sortBy}/{@code sortDir}
     * (defaults: {@code fullName asc}).
     */
    @Operation(summary = "Listar usuarios", description = "Obtiene un listado de usuarios con paginación y ordenamiento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserListResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<UserListResponseDto> list(
            @Parameter(name = "sortBy", description = "Campo de ordenamiento: fullName | email | role | status | createdAt", example = "fullName")
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @Parameter(name = "sortDir", description = "Dirección: asc | desc", example = "asc")

            @RequestParam(value = "sortDir", required = false) String sortDir) {
        log.debug("GET /users sortBy={} sortDir={}", sortBy, sortDir);
        return ResponseEntity.ok(userService.list(sortBy, sortDir));
    }

    /**
     * USR-02: obtiene un usuario por id.
     */
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene los detalles de un usuario específico por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getById(
            @Parameter(name = "id", description = "ID del usuario", example = "1")
            @PathVariable Long id) {
        log.debug("GET /users/{}", id);
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * USR-03: crea un nuevo usuario (201).
     */
    @Operation(summary = "Crear nuevo usuario", description = "Crea un nuevo usuario con los datos proporcionados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateUserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto: email ya existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<CreateUserResponseDto> create(
            @Valid @RequestBody CreateUserRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("POST /users email={}", request.email());
        CreateUserResponseDto response = userService.create(request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * USR-04: edita un usuario existente.
     */
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateUserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto: email ya existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<UpdateUserResponseDto> update(
            @Parameter(name = "id", description = "ID del usuario", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("PUT /users/{}", id);
        UpdateUserResponseDto response = userService.update(id, request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * USR-05: activa o desactiva un usuario.
     */
    @Operation(summary = "Activar/Desactivar usuario", description = "Cambia el estado de un usuario a ACTIVE o INACTIVE por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateStatusResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (no ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<UpdateStatusResponseDto> changeStatus(
            @Parameter(name = "id", description = "ID del usuario", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("PATCH /users/{}/status newStatus={}", id, request.status());
        UpdateStatusResponseDto response = userService.changeStatus(id, request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }
}
