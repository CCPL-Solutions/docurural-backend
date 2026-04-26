package co.edu.docurural.shared.exception;

import co.edu.docurural.shared.dto.ApiErrorResponse;
import co.edu.docurural.user.dto.CreateUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("error.validation", Locale.getDefault(), "Errores de validación");
        messageSource.addMessage("error.bad-request-body", Locale.getDefault(), "El cuerpo de la solicitud es inválido o contiene un valor no permitido");
        messageSource.addMessage("auth.login.invalid-credentials", Locale.getDefault(), "Correo o contraseña incorrectos");
        messageSource.addMessage("auth.login.account-disabled", Locale.getDefault(), "Su cuenta ha sido desactivada. Contacte al administrador");
        messageSource.addMessage("auth.access-denied", Locale.getDefault(), "No tiene permisos para realizar esta acción");
        messageSource.addMessage("auth.session.expired", Locale.getDefault(), "Su sesión ha expirado por inactividad. Por favor inicie sesión nuevamente");
        messageSource.addMessage("error.internal-server", Locale.getDefault(), "Error inesperado del servidor");

        handler = new GlobalExceptionHandler(messageSource);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleValidation_buildsFieldErrorsList_status400() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createUserRequest");
        bindingResult.addError(new FieldError("createUserRequest", "email", "El correo electrónico es obligatorio"));
        bindingResult.addError(new FieldError("createUserRequest", "email", "No debe sobrescribir el primer mensaje"));
        bindingResult.addError(new FieldError("createUserRequest", "password", "La contraseña es obligatoria"));
        bindingResult.addError(new ObjectError("confirmPassword", "La confirmación de contraseña no coincide"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Errores de validación");
        assertThat(response.getBody().fieldErrors())
                .containsEntry("email", "El correo electrónico es obligatorio")
                .containsEntry("password", "La contraseña es obligatoria")
                .containsEntry("confirmPassword", "La confirmación de contraseña no coincide");
    }

    @Test
    void handleHttpMessageNotReadable_status400_genericMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON inválido",
                new IllegalArgumentException("enum value out of range")
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleHttpMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message())
                .isEqualTo("El cuerpo de la solicitud es inválido o contiene un valor no permitido");
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    void handleNotFound_status404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Usuario no encontrado con id 99"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Usuario no encontrado con id 99");
    }

    @Test
    void handleConflict_status409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("Ya existe un usuario registrado con este correo electrónico"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message())
                .isEqualTo("Ya existe un usuario registrado con este correo electrónico");
    }

    @Test
    void handleBusinessRule_withCustomStatus_respectsStatus() {
        ResponseEntity<ApiErrorResponse> badRequest = handler.handleBusinessRule(
                new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, "El usuario ya se encuentra en el estado solicitado"),
                request
        );

        ResponseEntity<ApiErrorResponse> forbidden = handler.handleBusinessRule(
                new BusinessRuleException(BusinessErrorCode.FORBIDDEN, "No puede desactivar su propia cuenta"),
                request
        );

        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getBody()).isNotNull();
        assertThat(badRequest.getBody().status()).isEqualTo(400);
        assertThat(badRequest.getBody().message())
                .isEqualTo("El usuario ya se encuentra en el estado solicitado");

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody()).isNotNull();
        assertThat(forbidden.getBody().status()).isEqualTo(403);
        assertThat(forbidden.getBody().message()).isEqualTo("No puede desactivar su propia cuenta");
    }

    @Test
    void handleBadCredentials_status401_spanishMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(
                new BadCredentialsException("bad credentials"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Correo o contraseña incorrectos");
    }

    @Test
    void handleDisabled_status403_spanishMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDisabled(
                new DisabledException("disabled"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message())
                .isEqualTo("Su cuenta ha sido desactivada. Contacte al administrador");
    }

    @Test
    void handleAccessDenied_status403() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("forbidden"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("No tiene permisos para realizar esta acción");
    }

    @Test
    void handleAuthenticationFallback_status401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAuthentication(
                new AuthenticationServiceException("auth error"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message())
                .isEqualTo("Su sesión ha expirado por inactividad. Por favor inicie sesión nuevamente");
    }

    @Test
    void handleGeneric_status500_doesNotLeakInternalDetails() {
        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(
                new RuntimeException("DB password leaked: secret-123"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Error inesperado del servidor");
        assertThat(response.getBody().message()).doesNotContain("secret-123");
    }

    private static MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("dummyEndpoint", CreateUserRequest.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private static void dummyEndpoint(CreateUserRequest request) {
        // Intencionalmente vacío: solo se usa para construir MethodParameter en tests.
    }
}

