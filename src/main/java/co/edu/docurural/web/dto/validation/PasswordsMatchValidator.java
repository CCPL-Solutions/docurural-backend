package co.edu.docurural.web.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.RecordComponent;
import java.util.Objects;

/**
 * Validador asociado a {@link PasswordsMatch}.
 *
 * <p>Funciona tanto con records como con clases comunes: accede a los campos
 * mediante el accessor del record o, como fallback, por reflexión de campos
 * declarados. Si {@code password} es {@code null} o está en blanco la
 * validación pasa (se asume que {@code @NotBlank} en el DTO decide si es
 * obligatorio). Cuando {@code password} tiene valor, el validador exige que
 * {@code confirmPassword} sea exactamente igual.
 */
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;
    private String message;

    @Override
    public void initialize(PasswordsMatch annotation) {
        this.passwordField = annotation.passwordField();
        this.confirmPasswordField = annotation.confirmPasswordField();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object target, ConstraintValidatorContext context) {
        if (target == null) {
            return true;
        }
        String password = readField(target, passwordField);
        String confirmPassword = readField(target, confirmPasswordField);

        // Si no se envió password, no hay nada que validar aquí (se deja a @NotBlank si corresponde).
        if (password == null || password.isBlank()) {
            return true;
        }
        if (Objects.equals(password, confirmPassword)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(confirmPasswordField)
                .addConstraintViolation();
        return false;
    }

    private String readField(Object target, String fieldName) {
        Class<?> type = target.getClass();
        try {
            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    if (component.getName().equals(fieldName)) {
                        Object value = component.getAccessor().invoke(target);
                        return value == null ? null : value.toString();
                    }
                }
                return null;
            }
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "No se pudo leer el campo '" + fieldName + "' en " + type.getName(), ex);
        }
    }
}
