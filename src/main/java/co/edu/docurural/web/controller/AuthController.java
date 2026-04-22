package co.edu.docurural.web.controller;

import co.edu.docurural.service.AuthService;
import co.edu.docurural.web.dto.auth.LoginRequest;
import co.edu.docurural.web.dto.auth.LoginResponse;
import co.edu.docurural.web.dto.common.MessageResponse;
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
public class AuthController {

    private final AuthService authService;

    /**
     * AUTH-01: inicio de sesión público.
     *
     * @param request     credenciales validadas con Bean Validation.
     * @param httpRequest petición HTTP (para resolver la IP del registro de auditoría).
     * @return 200 {@link LoginResponse} con token, tipo, expiración y resumen del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.debug("POST /auth/login email={}", request.email());
        LoginResponse response = authService.login(request, httpRequest);
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
     * @return 200 {@link MessageResponse} con el mensaje de confirmación.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest httpRequest) {
        log.debug("POST /auth/logout");
        MessageResponse response = authService.logout(httpRequest);
        return ResponseEntity.ok(response);
    }
}
