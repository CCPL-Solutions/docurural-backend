package co.edu.docurural.web.exception;

import co.edu.docurural.web.dto.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador centralizado de excepciones para toda la capa web.
 *
 * <p>Traduce cualquier {@link Throwable} propagado desde controllers/services/filtros
 * a la estructura estandar {@link ApiErrorResponse} definida en
 * {@code docs/api-rest-sprint1.md} seccion 1.2:
 *
 * <pre>
 * {
 *   "timestamp": "2026-04-17T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Descripcion legible del error",
 *   "fieldErrors": { "campo": "mensaje" }   // solo en validacion
 * }
 * </pre>
 *
 * <p>Principios aplicados:
 * <ul>
 *   <li>Los mensajes siempre estan en espanol y coinciden literalmente con los
 *       textos del contrato de API.</li>
 *   <li>Los errores 4xx se logean como {@code warn}/{@code debug}; los 5xx como
 *       {@code error} con stack trace completo.</li>
 *   <li>El mensaje del 500 nunca expone detalles internos: solo
 *       "Error inesperado del servidor". El detalle real vive en los logs.</li>
 *   <li>{@code AuthenticationException} y {@code AccessDeniedException} lanzadas
 *       por {@code @PreAuthorize} dentro de un controller llegan aqui; las
 *       producidas antes de entrar al controller (filtros JWT) las atienden el
 *       {@code AuthenticationEntryPoint} y el {@code AccessDeniedHandler}
 *       declarados en {@code SecurityConfig}, que emiten el mismo formato JSON.</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    static final String MSG_VALIDATION_ERRORS = "Errores de validacion";
    static final String MSG_BAD_REQUEST_BODY =
            "El cuerpo de la solicitud es invalido o contiene un valor no permitido";
    static final String MSG_BAD_CREDENTIALS = "Correo o contrasena incorrectos";
    static final String MSG_ACCOUNT_DISABLED =
            "Su cuenta ha sido desactivada. Contacte al administrador";
    static final String MSG_ACCESS_DENIED = "No tiene permisos para realizar esta accion";
    static final String MSG_SESSION_EXPIRED =
            "Su sesion ha expirado por inactividad. Por favor inicie sesion nuevamente";
    static final String MSG_INTERNAL_SERVER_ERROR = "Error inesperado del servidor";

    /**
     * {@link MethodArgumentNotValidException} es lanzada por {@code @Valid} en request bodies.
     * Se traduce a 400 con el mapa {@code fieldErrors} extraido del {@link org.springframework.validation.BindingResult}.
     * Si hay errores globales (p.ej. {@code @PasswordsMatch} a nivel de clase) se agregan
     * con la clave {@code _global} o el nombre del campo objetivo del {@code ObjectError}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Preservar el primer mensaje por campo si hay varias violaciones.
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            String key = globalError.getObjectName() == null ? "_global" : globalError.getObjectName();
            fieldErrors.putIfAbsent(key, globalError.getDefaultMessage());
        });

        log.warn("Validacion fallida en {} {}: {}",
                request.getMethod(), request.getRequestURI(), fieldErrors);

        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                MSG_VALIDATION_ERRORS,
                fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * {@link HttpMessageNotReadableException} cubre cuerpos JSON mal formados y
     * valores invalidos de enum que Jackson rechaza antes de llegar a Bean Validation
     * (p. ej. {@code {"status":"FOO"}} en {@code PATCH /users/{id}/status}).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo de peticion invalido en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, MSG_BAD_REQUEST_BODY);
    }

    /**
     * Recursos no encontrados por id (p. ej. usuario inexistente).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Conflictos de unicidad (p. ej. email ya registrado al crear o editar un usuario).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        log.warn("Conflicto en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Violacion de regla de negocio. El status HTTP viaja como campo de la propia
     * excepcion (400 o 403 segun el caso: auto-rol, auto-desactivacion, estado
     * duplicado, ordenamiento invalido, etc.).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        log.warn("Regla de negocio violada en {} {} [{}]: {}",
                request.getMethod(), request.getRequestURI(), status.value(), ex.getMessage());
        return buildResponse(status, ex.getMessage());
    }

    /**
     * Credenciales invalidas (email inexistente o contrasena incorrecta) -> 401
     * con el mensaje exacto del contrato AUTH-01.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Credenciales invalidas en {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_BAD_CREDENTIALS);
    }

    /**
     * Cuenta desactivada (status = INACTIVE) -> 403 con el mensaje exacto del contrato AUTH-01.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Intento de login con cuenta desactivada en {} {}",
                request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, MSG_ACCOUNT_DISABLED);
    }

    /**
     * Sin permisos suficientes (rechazo de {@code @PreAuthorize}) -> 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, MSG_ACCESS_DENIED);
    }

    /**
     * Fallback para cualquier otra {@link AuthenticationException} que escape
     * del {@code AuthenticationEntryPoint} (poco probable, pero se cubre para
     * no caer al handler generico 500 y preservar el mensaje estandar de
     * sesion expirada).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Excepcion de autenticacion en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, MSG_SESSION_EXPIRED);
    }

    /**
     * Fallback de ultimo recurso. Cualquier otra excepcion es un bug: se logea
     * con stack trace completo pero al cliente solo se le devuelve el mensaje
     * generico para no filtrar detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, MSG_INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(body);
    }
}
