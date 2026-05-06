package co.edu.docurural.user.mapper;

import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.auth.dto.UserSummary;
import co.edu.docurural.user.dto.CreateUserResponse;
import co.edu.docurural.user.dto.UpdateStatusResponse;
import co.edu.docurural.user.dto.UpdateUserResponse;
import co.edu.docurural.user.dto.UserListResponse;
import co.edu.docurural.user.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserMapperTest {

    @Test
    void toResponse_mapsAllFields() {
        User user = TestFixtures.userAdmin(5L);

        UserResponse response = UserMapper.toResponse(user);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.fullName()).isEqualTo("Ana Admin");
        assertThat(response.email()).isEqualTo("ana.admin@docurural.edu.co");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
        assertThat(response.lastLogin()).isNull();
    }

    @Test
    void toResponse_outputDoesNotExposePasswordHash() {
        // UserResponse is a record; its declared fields are exactly its components.
        // Verify that none of them is named 'passwordHash'.
        String[] fieldNames = java.util.Arrays.stream(UserResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toArray(String[]::new);

        assertThat(fieldNames)
                .as("UserResponse must not expose passwordHash")
                .doesNotContain("passwordHash");
    }

    @Test
    void toSummary_returnsOnlyIdFullNameEmailRole() {
        User user = TestFixtures.userEditor(10L);

        UserSummary summary = UserMapper.toSummary(user);

        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.fullName()).isEqualTo("Erik Editor");
        assertThat(summary.email()).isEqualTo("erik.editor@docurural.edu.co");
        assertThat(summary.role()).isEqualTo("EDITOR");
    }

    @Test
    void toListResponse_withMultipleUsers_setsTotalCorrectly() {
        List<User> users = List.of(
                TestFixtures.userAdmin(1L),
                TestFixtures.userEditor(2L),
                TestFixtures.userInactive(3L)
        );

        UserListResponse response = UserMapper.toListResponse(users);

        assertThat(response.totalUsers()).isEqualTo(3);
        assertThat(response.users()).hasSize(3);
        assertThat(response.users())
                .extracting(UserResponse::id)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void toListResponse_emptyList_totalZero() {
        List<User> emptyUsers = new ArrayList<>();

        UserListResponse response = UserMapper.toListResponse(emptyUsers);

        assertThat(response.totalUsers()).isZero();
        assertThat(response.users()).isEmpty();
    }

    @Test
    void toCreateResponse_includesMessage() {
        User user = TestFixtures.userEditor(15L);
        String expectedMessage = "user.created.success";

        CreateUserResponse response = UserMapper.toCreateResponse(user, expectedMessage);

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.fullName()).isEqualTo("Erik Editor");
        assertThat(response.email()).isEqualTo("erik.editor@docurural.edu.co");
        assertThat(response.role()).isEqualTo("EDITOR");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
        assertThat(response.message()).isEqualTo(expectedMessage);
    }

    @Test
    void toUpdateResponse_includesMessage() {
        User user = TestFixtures.userAdmin(20L);
        String expectedMessage = "user.updated.success";

        UpdateUserResponse response = UserMapper.toUpdateResponse(user, expectedMessage);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.fullName()).isEqualTo("Ana Admin");
        assertThat(response.email()).isEqualTo("ana.admin@docurural.edu.co");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo(expectedMessage);
    }

    @Test
    void toStatusResponse_onlyIncludesIdFullNameStatusMessage() {
        User user = TestFixtures.userInactive(25L);
        String expectedMessage = "user.deactivated.success";

        UpdateStatusResponse response = UserMapper.toStatusResponse(user, expectedMessage);

        assertThat(response.id()).isEqualTo(25L);
        assertThat(response.fullName()).isEqualTo("Ida Inactive");
        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.message()).isEqualTo(expectedMessage);
    }

    @Test
    void toResponse_withNullUser_throwsNullPointerException() {
        assertThatThrownBy(() -> UserMapper.toResponse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toSummary_withNullUser_throwsNullPointerException() {
        assertThatThrownBy(() -> UserMapper.toSummary(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toListResponse_withNullList_throwsNullPointerException() {
        assertThatThrownBy(() -> UserMapper.toListResponse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toCreateResponse_withNullUser_throwsNullPointerException() {
        assertThatThrownBy(() -> UserMapper.toCreateResponse(null, "message"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toUpdateResponse_withNullUser_throwsNullPointerException() {
        assertThatThrownBy(() -> UserMapper.toUpdateResponse(null, "message"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStatusResponse_withNullUser_throwsNullPointerException() {
        assertThatThrownBy(() -> UserMapper.toStatusResponse(null, "message"))
                .isInstanceOf(NullPointerException.class);
    }
}

