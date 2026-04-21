package co.edu.docurural.web.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotacion a nivel de clase que valida que dos campos tipo {@link String}
 * ({@code password} y {@code confirmPassword}) coincidan.
 *
 * <p>La validacion solo se ejecuta cuando {@code password} es no nulo y no vacio.
 * Esto permite reutilizar la anotacion tanto para la creacion (contrasena
 * obligatoria) como para la edicion (contrasena opcional); el hecho de que
 * {@code password} sea obligatoria o no se controla con {@code @NotBlank} en el
 * DTO correspondiente.
 *
 * <p>El error se reporta en el campo {@code confirmPassword} para que el
 * frontend lo muestre junto al control correcto.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsMatchValidator.class)
public @interface PasswordsMatch {

    String message() default "La confirmacion de contrasena no coincide";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Nombre del campo contrasena en el DTO anotado.
     */
    String passwordField() default "password";

    /**
     * Nombre del campo de confirmacion en el DTO anotado.
     */
    String confirmPasswordField() default "confirmPassword";
}
