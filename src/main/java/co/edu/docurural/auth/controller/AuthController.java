package co.edu.docurural.auth.controller;

import co.edu.docurural.auth.service.AuthService;
import co.edu.docurural.auth.dto.LoginRequestDto;
import co.edu.docurural.auth.dto.LoginResponseDto;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.dto.ApiErrorResponseDto;
import co.edu.docurural.shared.dto.MessageResponseDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del módulo de Autenticación (AUTH-01, AUTH-02).
 *
 * <p>Expone los endpoints públicos y autenticados de login/logout. El
 * {@code context-path} {@code /api} se configura globalmente en
 * {@code application.yaml}, por eso aquí el mapping solo incluye {@code /auth}.
 *
 * <p>Toda la lógica de negocio, validación de credenciales, generación del
 * token JWT y registro en {@code activity_log} se delega a {@link AuthService}.
 * Los errores se propagan al {@code GlobalExceptionHandler} (Fase 7) que los
 * traduce al formato estándar de la API.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Login y logout — AUTH-01, AUTH-02")
public class AuthController {

    private final AuthService authService;
    private final AuditContextResolver auditContextResolver;

    /**
     * AUTH-01: inicio de sesión público.
     *
     * @param request     credenciales validadas con Bean Validation.
     * @param httpRequest petición HTTP (para resolver la IP del registro de auditoría).
     * @return 200 {@link LoginResponseDto} con token, tipo, expiración y resumen del usuario.
     */
    @Operation(summary = "Iniciar sesión", description = "Autentica con email y contraseña. Devuelve token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        log.debug("POST /auth/login email={}", request.email());
        LoginResponseDto response = authService.login(request, auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }

    /**
     * AUTH-02: cierre de sesión del usuario autenticado.
     *
     * <p>Al ser JWT stateless, el cliente invalida el token localmente; este
     * endpoint existe para registrar la acción {@code LOGOUT} en
     * {@code activity_log}.
     *
     * @param httpRequest petición HTTP (para resolver la IP del registro de auditoría).
     * @return 200 {@link MessageResponseDto} con el mensaje de confirmación.
     */
    @Operation(summary = "Cerrar sesión", description = "Registra el logout en el log de actividad. El cliente invalida el token localmente.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o expirado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponseDto.class)))
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponseDto> logout(HttpServletRequest httpRequest) {
        log.debug("POST /auth/logout");
        MessageResponseDto response = authService.logout(auditContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(response);
    }
}
