package co.edu.docurural.user.dto;

import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.enums.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cota de longitud de {@code confirmPassword} (Principio IX: todo componente de un
 * {@code RequestDto} lleva al menos una restricción que acote su valor).
 */
class UpdateUserRequestDtoTest {

    private static final String FULL_NAME = "María García López";
    private static final String EMAIL = "maria.garcia@docurural.edu.co";
    private static final int MAX_CONFIRM_PASSWORD_LENGTH = 128;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validate_reportsSizeViolation_whenConfirmPasswordExceedsMaxLength() {
        String tooLong = "A1a!".repeat(MAX_CONFIRM_PASSWORD_LENGTH);
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                FULL_NAME, EMAIL, UserRole.READER, tooLong, tooLong);

        Set<ConstraintViolation<UpdateUserRequestDto>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("confirmPassword");
    }

    @Test
    void validate_passesSizeCheck_whenConfirmPasswordIsNull() {
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                FULL_NAME, EMAIL, UserRole.READER);

        Set<ConstraintViolation<UpdateUserRequestDto>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
