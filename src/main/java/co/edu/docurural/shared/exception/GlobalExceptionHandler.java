package co.edu.docurural.shared.exception;

import co.edu.docurural.shared.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador centralizado de excepciones para toda la capa web.
 *
 * <p>Traduce cualquier {@link Throwable} propagado desde controllers/services/filtros
 * a la estructura estándar {@link ApiErrorResponse} definida en
 * {@code docs/api-rest-sprint1.md} sección 1.2:
 *
 * <pre>
 * {
 *   "timestamp": "2026-04-17T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Descripción legible del error",
 *   "fieldErrors": { "campo": "mensaje" }   // solo en validación
 * }
 * </pre>
 *
 * <p>Principios aplicados:
 * <ul>
 *   <li>Los mensajes siempre están en español y coinciden literalmente con los
 *       textos del contrato de API.</li>
 *   <li>Los errores 4xx se logean como {@code warn}/{@code debug}; los 5xx como
 *       {@code error} con stack trace completo.</li>
 *   <li>El mensaje del 500 nunca expone detalles internos: solo
 *       "Error inesperado del servidor". El detalle real vive en los logs.</li>
 *   <li>{@code AuthenticationException} y {@code AccessDeniedException} lanzadas
 *       por {@code @PreAuthorize} dentro de un controller llegan aquí; las
 *       producidas antes de entrar al controller (filtros JWT) las atienden el
 *       {@code AuthenticationEntryPoint} y el {@code AccessDeniedHandler}
 *       declarados en {@code SecurityConfig}, que emiten el mismo formato JSON.</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * {@link MethodArgumentNotValidException} es lanzada por {@code @Valid} en request bodies.
     * Se traduce a 400 con el mapa {@code fieldErrors} extraído del {@link org.springframework.validation.BindingResult}.
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

        log.warn("Validación fallida en {} {}: {}",
                request.getMethod(), request.getRequestURI(), fieldErrors);

        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                resolve("error.validation"),
                fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * {@link HttpMessageNotReadableException} cubre cuerpos JSON mal formados y
     * valores inválidos de enum que Jackson rechaza antes de llegar a Bean Validation
     * (p. ej. {@code {"status":"FOO"}} en {@code PATCH /users/{id}/status}).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo de petición inválido en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, resolve("error.bad-request-body"));
    }

    /**
     * Archivo multipart supera el límite configurado en {@code spring.servlet.multipart.max-file-size} -> 413.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Archivo demasiado grande en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, resolve("document.file.too-large"));
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
     * Violación de regla de negocio. Solo aquí se mapea código de dominio a HTTP.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        HttpStatus status = mapBusinessErrorCode(ex.getCode());
        log.warn("Regla de negocio violada en {} {} [{}]: {}",
                request.getMethod(), request.getRequestURI(), status.value(), ex.getMessage());
        return buildResponse(status, ex.getMessage());
    }

    /**
     * Credenciales inválidas (email inexistente o contraseña incorrecta) -> 401
     * con el mensaje exacto del contrato AUTH-01.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Credenciales inválidas en {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, resolve("auth.login.invalid-credentials"));
    }

    /**
     * Cuenta desactivada (status = INACTIVE) -> 403 con el mensaje exacto del contrato AUTH-01.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Intento de login con cuenta desactivada en {} {}",
                request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, resolve("auth.login.account-disabled"));
    }

    /**
     * Sin permisos suficientes (rechazo de {@code @PreAuthorize}) -> 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, resolve("auth.access-denied"));
    }

    /**
     * Fallback para cualquier otra {@link AuthenticationException} que escape
     * del {@code AuthenticationEntryPoint} (poco probable, pero se cubre para
     * no caer al handler genérico 500 y preservar el mensaje estándar de
     * sesión expirada).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Excepción de autenticación en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, resolve("auth.session.expired"));
    }

    /**
     * Fallo de infraestructura al escribir un archivo en disco -> 500 con mensaje contractual.
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleFileStorage(
            FileStorageException ex, HttpServletRequest request) {
        log.error("Fallo de almacenamiento en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Fallback de último recurso. Cualquier otra excepción es un bug: se logea
     * con stack trace completo pero al cliente solo se le devuelve el mensaje
     * genérico para no filtrar detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, resolveInternalError());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus mapBusinessErrorCode(BusinessErrorCode code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (code) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case PAYLOAD_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        };
    }

    private String resolve(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Resuelve el mensaje del 500 con un fallback de último recurso si la clave
     * {@code error.internal-server} no existiera en {@code messages.properties}.
     * Es la única cadena que sobrevive como literal en el código.
     */
    private String resolveInternalError() {
        try {
            return resolve("error.internal-server");
        } catch (NoSuchMessageException ex) {
            return "Error inesperado del servidor";
        }
    }
}
