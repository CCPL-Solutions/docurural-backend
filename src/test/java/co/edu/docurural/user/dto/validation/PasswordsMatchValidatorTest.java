package co.edu.docurural.user.dto.validation;

import co.edu.docurural.user.enums.UserRole;
import co.edu.docurural.user.dto.CreateUserRequestDto;
import co.edu.docurural.user.dto.UpdateUserRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordsMatchValidatorTest {

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
    void isValid_nullTarget_returnsTrue() {
        PasswordsMatchValidator validatorUnderTest = new PasswordsMatchValidator();

        boolean valid = validatorUnderTest.isValid(null, null);

        assertThat(valid).isTrue();
    }

    @Test
    void isValid_recordWithMatchingPasswords_passes() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "Ana Admin",
                "ana.admin@docurural.edu.co",
                "Password123!",
                "Password123!",
                UserRole.ADMIN
        );

        Set<ConstraintViolation<CreateUserRequestDto>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_recordWithMismatchedPasswords_failsOnConfirmPasswordField() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "Ana Admin",
                "ana.admin@docurural.edu.co",
                "password123",
                "password999",
                UserRole.ADMIN
        );

        Set<ConstraintViolation<CreateUserRequestDto>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("confirmPassword");
                    assertThat(v.getMessageTemplate()).isEqualTo("{validation.user.confirm-password.mismatch}");
                });
    }

    @Test
    void isValid_recordWithBlankPassword_passes() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "Ana Admin",
                "ana.admin@docurural.edu.co",
                "   ",
                "different123",
                UserRole.ADMIN
        );

        Set<ConstraintViolation<CreateUserRequestDto>> violations = validator.validate(request);

        // En create, password en blanco debe fallar por @NotBlank/@Size, no por PasswordsMatch.
        assertThat(violations)
                .isNotEmpty()
                .noneSatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("confirmPassword");
                    assertThat(v.getMessageTemplate()).isEqualTo("{validation.user.confirm-password.mismatch}");
                });
    }

    @Test
    void isValid_updateRequestWithNullPassword_passes() {
        UpdateUserRequestDto request = new UpdateUserRequestDto(
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                UserRole.EDITOR,
                null,
                null
        );

        Set<ConstraintViolation<UpdateUserRequestDto>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_updateRequestWithPasswordButMismatchedConfirm_fails() {
        UpdateUserRequestDto request = new UpdateUserRequestDto(
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                UserRole.EDITOR,
                "newpass123",
                "another999"
        );

        Set<ConstraintViolation<UpdateUserRequestDto>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("confirmPassword");
                    assertThat(v.getMessageTemplate()).isEqualTo("{validation.user.confirm-password.mismatch}");
                });
    }
}

