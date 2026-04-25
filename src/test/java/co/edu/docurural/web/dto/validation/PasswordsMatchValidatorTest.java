package co.edu.docurural.web.dto.validation;

import co.edu.docurural.domain.enums.UserRole;
import co.edu.docurural.web.dto.user.CreateUserRequest;
import co.edu.docurural.web.dto.user.UpdateUserRequest;
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
        CreateUserRequest request = new CreateUserRequest(
                "Ana Admin",
                "ana.admin@docurural.edu.co",
                "password123",
                "password123",
                UserRole.ADMIN
        );

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_recordWithMismatchedPasswords_failsOnConfirmPasswordField() {
        CreateUserRequest request = new CreateUserRequest(
                "Ana Admin",
                "ana.admin@docurural.edu.co",
                "password123",
                "password999",
                UserRole.ADMIN
        );

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("confirmPassword");
                    assertThat(v.getMessageTemplate()).isEqualTo("{validation.user.confirm-password.mismatch}");
                });
    }

    @Test
    void isValid_recordWithBlankPassword_passes() {
        CreateUserRequest request = new CreateUserRequest(
                "Ana Admin",
                "ana.admin@docurural.edu.co",
                "   ",
                "different123",
                UserRole.ADMIN
        );

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

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
        UpdateUserRequest request = new UpdateUserRequest(
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                UserRole.EDITOR,
                null,
                null
        );

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_updateRequestWithPasswordButMismatchedConfirm_fails() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                UserRole.EDITOR,
                "newpass123",
                "another999"
        );

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("confirmPassword");
                    assertThat(v.getMessageTemplate()).isEqualTo("{validation.user.confirm-password.mismatch}");
                });
    }
}

