package co.edu.docurural.web.controller;

import co.edu.docurural.config.security.CustomUserPrincipal;
import co.edu.docurural.service.UserService;
import co.edu.docurural.web.dto.user.CreateUserRequest;
import co.edu.docurural.web.dto.user.CreateUserResponse;
import co.edu.docurural.web.dto.user.UpdateStatusRequest;
import co.edu.docurural.web.dto.user.UpdateStatusResponse;
import co.edu.docurural.web.dto.user.UpdateUserRequest;
import co.edu.docurural.web.dto.user.UpdateUserResponse;
import co.edu.docurural.web.dto.user.UserListResponse;
import co.edu.docurural.web.dto.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * en {@link UserService}. El {@code adminId} se extrae del
 * {@link SecurityContextHolder} vía {@link CustomUserPrincipal}.
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * USR-01: listado de usuarios ordenado por {@code sortBy}/{@code sortDir}
     * (defaults: {@code fullName asc}).
     */
    @GetMapping
    public ResponseEntity<UserListResponse> list(
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        log.debug("GET /users sortBy={} sortDir={}", sortBy, sortDir);
        return ResponseEntity.ok(userService.list(sortBy, sortDir));
    }

    /**
     * USR-02: obtiene un usuario por id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        log.debug("GET /users/{}", id);
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * USR-03: crea un nuevo usuario (201).
     */
    @PostMapping
    public ResponseEntity<CreateUserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = currentAdminId();
        log.debug("POST /users adminId={} email={}", adminId, request.email());
        CreateUserResponse response = userService.create(request, adminId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * USR-04: edita un usuario existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UpdateUserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = currentAdminId();
        log.debug("PUT /users/{} adminId={}", id, adminId);
        UpdateUserResponse response = userService.update(id, request, adminId, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * USR-05: activa o desactiva un usuario.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UpdateStatusResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            HttpServletRequest httpRequest) {
        Long adminId = currentAdminId();
        log.debug("PATCH /users/{}/status adminId={} newStatus={}",
                id, adminId, request.status());
        UpdateStatusResponse response = userService.changeStatus(id, request, adminId, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene el id del administrador autenticado desde el {@code SecurityContext}.
     *
     * <p>El filtro JWT siempre establece un {@link CustomUserPrincipal} como
     * {@code principal}; si por alguna razón eso no ocurre se lanza
     * {@link IllegalStateException} (síntoma de un bug en la cadena de
     * seguridad, no un error de negocio).
     */
    private Long currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "No hay un usuario autenticado en el contexto de seguridad");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
            throw new IllegalStateException(
                    "El principal no es una instancia de CustomUserPrincipal: "
                            + (principal == null ? "null" : principal.getClass().getName()));
        }
        return customPrincipal.getId();
    }
}
