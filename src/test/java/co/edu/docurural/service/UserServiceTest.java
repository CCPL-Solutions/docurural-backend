package co.edu.docurural.service;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.ActivityAction;
import co.edu.docurural.domain.enums.enums.UserRole;
import co.edu.docurural.domain.enums.enums.UserStatus;
import co.edu.docurural.domain.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.web.dto.user.CreateUserRequest;
import co.edu.docurural.web.dto.user.CreateUserResponse;
import co.edu.docurural.web.dto.user.UpdateStatusRequest;
import co.edu.docurural.web.dto.user.UpdateStatusResponse;
import co.edu.docurural.web.dto.user.UpdateUserRequest;
import co.edu.docurural.web.dto.user.UpdateUserResponse;
import co.edu.docurural.web.dto.user.UserListResponse;
import co.edu.docurural.web.dto.user.UserResponse;
import co.edu.docurural.web.exception.BusinessRuleException;
import co.edu.docurural.web.exception.ConflictException;
import co.edu.docurural.web.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageSource messageSource;
    @Mock
    HttpServletRequest httpRequest;

    @InjectMocks
    UserService userService;

    @BeforeEach
    void stubMessageSource() {
        lenient().when(messageSource.getMessage(anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // list()
    // ------------------------------------------------------------------

    @Test
    void list_withDefaultParams_sortsByFullNameAsc() {
        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(TestFixtures.userAdmin(1L), TestFixtures.userEditor(2L)));

        UserListResponse response = userService.list(null, null);

        assertThat(response.totalUsers()).isEqualTo(2);
        assertThat(response.users()).hasSize(2);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(userRepository).findAll(sortCaptor.capture());
        Sort captured = sortCaptor.getValue();
        assertThat(captured.getOrderFor("fullName")).isNotNull();
        assertThat(captured.getOrderFor("fullName").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void list_withValidSortParams_delegatesToRepositoryWithCorrectSort() {
        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(TestFixtures.userAdmin(1L)));

        userService.list("email", "DESC");

        verify(userRepository).findAll(eq(Sort.by(Sort.Direction.DESC, "email")));
    }

    @Test
    void list_withBlankParams_appliesDefaults() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of());

        userService.list("   ", "  ");

        verify(userRepository).findAll(eq(Sort.by(Sort.Direction.ASC, "fullName")));
    }

    @Test
    void list_withUnsupportedSortField_throwsBusinessRule400() {
        assertThatThrownBy(() -> userService.list("password", "asc"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(userRepository);
    }

    @Test
    void list_withInvalidSortDirection_throwsBusinessRule400() {
        assertThatThrownBy(() -> userService.list("fullName", "sideways"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(userRepository);
    }

    // ------------------------------------------------------------------
    // findById()
    // ------------------------------------------------------------------

    @Test
    void findById_existing_returnsMappedResponse() {
        User editor = TestFixtures.userEditor(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(editor));

        UserResponse response = userService.findById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.fullName()).isEqualTo(editor.getFullName());
        assertThat(response.email()).isEqualTo(editor.getEmail());
        assertThat(response.role()).isEqualTo("EDITOR");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void findById_missing_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    void create_withUniqueEmail_encodesPassword_persists_logsCreateUser_returnsMessage() {
        CreateUserRequest request = TestFixtures.createUserRequest(
                "New Reader", "new.reader@docurural.edu.co", "plainpass1", UserRole.READER);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode("plainpass1")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        CreateUserResponse response = userService.create(request, ADMIN_ID, httpRequest);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.role()).isEqualTo("READER");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("user.created.success");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$encoded");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getFullName()).isEqualTo("New Reader");
        assertThat(saved.getRole()).isEqualTo(UserRole.READER);

        verify(activityLogService).record(
                eq(ActivityAction.CREATE_USER),
                eq(ADMIN_ID),
                isNull(),
                eq("Usuario creado: 50"),
                same(httpRequest));
    }

    @Test
    void create_withMismatchedPasswords_throwsBusinessRule400() {
        CreateUserRequest request = TestFixtures.createUserRequest(
                "Name", "name@docurural.edu.co", "password1", "password2", UserRole.EDITOR);

        assertThatThrownBy(() -> userService.create(request, ADMIN_ID, httpRequest))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(userRepository, passwordEncoder, activityLogService);
    }

    @Test
    void create_withExistingEmail_throwsConflict() {
        CreateUserRequest request = TestFixtures.createUserRequest(
                "Existing", "existing@docurural.edu.co", "plainpass1", UserRole.EDITOR);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request, ADMIN_ID, httpRequest))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Test
    void update_withNoChanges_savesUser_logsEmptyModifiedFieldsList() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                existing.getFullName(), existing.getEmail(), existing.getRole());

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserResponse response = userService.update(20L, request, ADMIN_ID, httpRequest);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.message()).isEqualTo("user.updated.success");

        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), any());
        verifyNoInteractions(passwordEncoder);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(ADMIN_ID),
                isNull(),
                eq("Campos modificados: []"),
                same(httpRequest));
    }

    @Test
    void update_changingEmailToExisting_throwsConflict() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                existing.getFullName(), "already.taken@docurural.edu.co", existing.getRole());

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("already.taken@docurural.edu.co", 20L))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.update(20L, request, ADMIN_ID, httpRequest))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_missingUser_throwsResourceNotFound() {
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                "Name", "mail@docurural.edu.co", UserRole.READER);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(404L, request, ADMIN_ID, httpRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_adminChangingOwnRole_throwsBusinessRule403() {
        User selfAdmin = TestFixtures.userAdmin(ADMIN_ID);
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                selfAdmin.getFullName(), selfAdmin.getEmail(), UserRole.EDITOR);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() -> userService.update(ADMIN_ID, request, ADMIN_ID, httpRequest))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_withNewPassword_mismatched_throwsBusinessRule400() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                existing.getFullName(), existing.getEmail(), existing.getRole(),
                "newpass12", "differentpass");

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.update(20L, request, ADMIN_ID, httpRequest))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_withNewPassword_reencodes_addsPasswordToModifiedFields() {
        User existing = TestFixtures.userEditor(20L);
        String originalHash = existing.getPasswordHash();
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                existing.getFullName(), existing.getEmail(), existing.getRole(),
                "newpass123", "newpass123");

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update(20L, request, ADMIN_ID, httpRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash())
                .isEqualTo("$2a$10$newEncoded")
                .isNotEqualTo(originalHash);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(ADMIN_ID),
                isNull(),
                eq("Campos modificados: [password]"),
                same(httpRequest));
    }

    @Test
    void update_withFullChangeSet_returnsAllModifiedFields() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequest request = TestFixtures.updateUserRequest(
                "Brand New Name",
                "new.mail@docurural.edu.co",
                UserRole.READER,
                "newpass123",
                "newpass123");

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("new.mail@docurural.edu.co", 20L))
                .thenReturn(false);
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$brandNew");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserResponse response = userService.update(20L, request, ADMIN_ID, httpRequest);

        assertThat(response.fullName()).isEqualTo("Brand New Name");
        assertThat(response.email()).isEqualTo("new.mail@docurural.edu.co");
        assertThat(response.role()).isEqualTo("READER");
        assertThat(response.message()).isEqualTo("user.updated.success");

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(ADMIN_ID),
                isNull(),
                eq("Campos modificados: [fullName, email, role, password]"),
                same(httpRequest));
    }

    // ------------------------------------------------------------------
    // changeStatus()
    // ------------------------------------------------------------------

    @Test
    void changeStatus_sameStatus_throwsBusinessRule400() {
        User existing = TestFixtures.userEditor(30L); // ACTIVE
        UpdateStatusRequest request = TestFixtures.updateStatusRequest(UserStatus.ACTIVE);

        when(userRepository.findById(30L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.changeStatus(30L, request, ADMIN_ID, httpRequest))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void changeStatus_adminDeactivatingSelf_throwsBusinessRule403() {
        User selfAdmin = TestFixtures.userAdmin(ADMIN_ID); // ACTIVE
        UpdateStatusRequest request = TestFixtures.updateStatusRequest(UserStatus.INACTIVE);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() -> userService.changeStatus(ADMIN_ID, request, ADMIN_ID, httpRequest))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void changeStatus_activateInactiveUser_persists_logsDeactivateUser_returnsActivatedMessage() {
        User inactive = TestFixtures.userInactive(40L);
        UpdateStatusRequest request = TestFixtures.updateStatusRequest(UserStatus.ACTIVE);

        when(userRepository.findById(40L)).thenReturn(Optional.of(inactive));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStatusResponse response = userService.changeStatus(40L, request, ADMIN_ID, httpRequest);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("user.activated.success");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(activityLogService).record(
                eq(ActivityAction.DEACTIVATE_USER),
                eq(ADMIN_ID),
                isNull(),
                eq("Nuevo estado: ACTIVE"),
                same(httpRequest));
    }

    @Test
    void changeStatus_deactivateActiveUser_persists_returnsDeactivatedMessage() {
        User active = TestFixtures.userEditor(41L);
        UpdateStatusRequest request = TestFixtures.updateStatusRequest(UserStatus.INACTIVE);

        when(userRepository.findById(41L)).thenReturn(Optional.of(active));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStatusResponse response = userService.changeStatus(41L, request, ADMIN_ID, httpRequest);

        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.message()).isEqualTo("user.deactivated.success");

        verify(activityLogService).record(
                eq(ActivityAction.DEACTIVATE_USER),
                eq(ADMIN_ID),
                isNull(),
                eq("Nuevo estado: INACTIVE"),
                same(httpRequest));
    }

    @Test
    void changeStatus_missingUser_throwsResourceNotFound() {
        UpdateStatusRequest request = TestFixtures.updateStatusRequest(UserStatus.INACTIVE);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeStatus(404L, request, ADMIN_ID, httpRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }
}
