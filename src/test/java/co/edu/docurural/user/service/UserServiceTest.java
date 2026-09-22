package co.edu.docurural.user.service;

import co.edu.docurural.user.dto.UpdateStatusResponseDto;
import co.edu.docurural.user.dto.UpdateUserResponseDto;
import co.edu.docurural.user.dto.UserResponseDto;
import co.edu.docurural.user.mapper.UserMapper;
import co.edu.docurural.activitylog.enums.ActivityAction;
import org.mapstruct.factory.Mappers;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserRole;
import co.edu.docurural.user.enums.UserStatus;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.dto.CreateUserRequestDto;
import co.edu.docurural.user.dto.CreateUserResponseDto;
import co.edu.docurural.user.dto.UpdateStatusRequestDto;
import co.edu.docurural.user.dto.UpdateUserRequestDto;
import co.edu.docurural.user.dto.UserListResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {


    private static final Long ADMIN_ID = 1L;
    private static final AuditContext AUDIT_ADMIN = new AuditContext(ADMIN_ID, "203.0.113.10");

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    ActivityLogService activityLogService;
    @Mock
    MessageResolver messageResolver;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageResolver.get(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        userService = new UserServiceImpl(userRepository, passwordEncoder,
                activityLogService, messageResolver, new SortingValidator(messageResolver),
                Mappers.getMapper(UserMapper.class));
    }

    // ------------------------------------------------------------------
    // list()
    // ------------------------------------------------------------------

    @Test
    void list_withDefaultParams_sortsByFullNameAsc() {
        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(TestFixtures.userAdmin(1L), TestFixtures.userEditor(2L)));

        UserListResponseDto response = userService.list(null, null);

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
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        verifyNoInteractions(userRepository);
    }

    @Test
    void list_withInvalidSortDirection_throwsBusinessRule400() {
        assertThatThrownBy(() -> userService.list("fullName", "sideways"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        verifyNoInteractions(userRepository);
    }

    // ------------------------------------------------------------------
    // findById()
    // ------------------------------------------------------------------

    @Test
    void findById_existing_returnsMappedResponse() {
        User editor = TestFixtures.userEditor(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(editor));

        UserResponseDto response = userService.findById(7L);

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
        CreateUserRequestDto request = TestFixtures.createUserRequest(
                "New Reader", "new.reader@docurural.edu.co", "plainpass1", UserRole.READER);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode("plainpass1")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        CreateUserResponseDto response = userService.create(request, AUDIT_ADMIN);

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
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Usuario creado: 50"));
    }

    @Test
    void create_withMismatchedPasswords_throwsBusinessRule400() {
        CreateUserRequestDto request = TestFixtures.createUserRequest(
                "Name", "name@docurural.edu.co", "password1", "password2", UserRole.EDITOR);

        assertThatThrownBy(() -> userService.create(request, AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        verifyNoInteractions(userRepository, passwordEncoder, activityLogService);
    }

    @Test
    void create_withExistingEmail_throwsConflict() {
        CreateUserRequestDto request = TestFixtures.createUserRequest(
                "Existing", "existing@docurural.edu.co", "plainpass1", UserRole.EDITOR);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request, AUDIT_ADMIN))
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
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                existing.getFullName(), existing.getEmail(), existing.getRole());

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserResponseDto response = userService.update(20L, request, AUDIT_ADMIN);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.message()).isEqualTo("user.updated.success");

        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), any());
        verifyNoInteractions(passwordEncoder);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: []"));
    }

    @Test
    void update_changingEmailToExisting_throwsConflict() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                existing.getFullName(), "already.taken@docurural.edu.co", existing.getRole());

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("already.taken@docurural.edu.co", 20L))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.update(20L, request, AUDIT_ADMIN))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_missingUser_throwsResourceNotFound() {
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                "Name", "mail@docurural.edu.co", UserRole.READER);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(404L, request, AUDIT_ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_adminChangingOwnRole_throwsBusinessRule403() {
        User selfAdmin = TestFixtures.userAdmin(ADMIN_ID);
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                selfAdmin.getFullName(), selfAdmin.getEmail(), UserRole.EDITOR);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() -> userService.update(ADMIN_ID, request, AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.FORBIDDEN));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_withNewPassword_mismatched_throwsBusinessRule400() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                existing.getFullName(), existing.getEmail(), existing.getRole(),
                "newpass12", "differentpass");

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.update(20L, request, AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, activityLogService);
    }

    @Test
    void update_withNewPassword_reencodes_addsPasswordToModifiedFields() {
        User existing = TestFixtures.userEditor(20L);
        String originalHash = existing.getPasswordHash();
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
                existing.getFullName(), existing.getEmail(), existing.getRole(),
                "newpass123", "newpass123");

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update(20L, request, AUDIT_ADMIN);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash())
                .isEqualTo("$2a$10$newEncoded")
                .isNotEqualTo(originalHash);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: [password]"));
    }

    @Test
    void update_withFullChangeSet_returnsAllModifiedFields() {
        User existing = TestFixtures.userEditor(20L);
        UpdateUserRequestDto request = TestFixtures.updateUserRequest(
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

        UpdateUserResponseDto response = userService.update(20L, request, AUDIT_ADMIN);

        assertThat(response.fullName()).isEqualTo("Brand New Name");
        assertThat(response.email()).isEqualTo("new.mail@docurural.edu.co");
        assertThat(response.role()).isEqualTo("READER");
        assertThat(response.message()).isEqualTo("user.updated.success");

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: [fullName, email, role, password]"));
    }


    // ------------------------------------------------------------------
    // canApprove (HU-32): US1 + US2
    // ------------------------------------------------------------------

    private static final String READER_APPROVER_KEY = "user.can-approve.reader-not-allowed";

    private void stubCreateDependencies() {
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });
    }

    private User createAndCaptureSaved(CreateUserRequestDto request) {
        stubCreateDependencies();
        userService.create(request, AUDIT_ADMIN);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private void stubExistingUserForUpdate(User existing) {
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UpdateUserResponseDto updateSameProfile(User existing, UserRole role, Boolean canApprove) {
        UpdateUserRequestDto request = new UpdateUserRequestDto(
                existing.getFullName(), existing.getEmail(), role, null, null, canApprove);
        return userService.update(existing.getId(), request, AUDIT_ADMIN);
    }

    @Test
    void create_setsCanApproveFalse_whenFieldOmitted() {
        User saved = createAndCaptureSaved(TestFixtures.createUserRequest(UserRole.EDITOR, null));

        assertThat(saved.isCanApprove()).isFalse();
    }

    @Test
    void create_persistsCanApproveTrue_whenRoleIsEditorAndFlagTrue() {
        User saved = createAndCaptureSaved(TestFixtures.createUserRequest(UserRole.EDITOR, true));

        assertThat(saved.isCanApprove()).isTrue();
    }

    @Test
    void create_leavesAdminWithoutPermission_whenFlagNotSent() {
        User saved = createAndCaptureSaved(TestFixtures.createUserRequest(UserRole.ADMIN, null));

        assertThat(saved.isCanApprove()).isFalse();
    }

    @Test
    void create_persistsCanApprove_whenAdminFlagged() {
        User saved = createAndCaptureSaved(TestFixtures.createUserRequest(UserRole.ADMIN, true));

        assertThat(saved.isCanApprove()).isTrue();
    }

    @Test
    void create_logsCanApprove_whenCreatedWithPermission() {
        createAndCaptureSaved(TestFixtures.createUserRequest(UserRole.EDITOR, true));

        verify(activityLogService).record(
                eq(ActivityAction.CREATE_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Usuario creado: 50; can_approve=true"));
    }

    @Test
    void create_acceptsReader_whenCanApproveFalseOrOmitted() {
        User withFalse = createAndCaptureSaved(TestFixtures.createUserRequest(UserRole.READER, false));

        assertThat(withFalse.isCanApprove()).isFalse();
        assertThat(userService.create(
                TestFixtures.createUserRequest(UserRole.READER, null), AUDIT_ADMIN).canApprove()).isFalse();
    }

    @Test
    void create_throwsBusinessRule_whenReaderWithCanApproveTrue() {
        CreateUserRequestDto request = TestFixtures.createUserRequest(UserRole.READER, true);

        assertThatThrownBy(() -> userService.create(request, AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(READER_APPROVER_KEY)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void update_setsCanApproveTrueAndLogsChange_whenEditorFlagged() {
        User existing = TestFixtures.userEditor(20L);
        stubExistingUserForUpdate(existing);

        UpdateUserResponseDto response = updateSameProfile(existing, UserRole.EDITOR, true);

        assertThat(response.canApprove()).isTrue();
        assertThat(existing.isCanApprove()).isTrue();
        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: []; can_approve: false → true"));
    }

    @Test
    void update_logsCanApproveLine_whenOnlyPermissionChanges() {
        User existing = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(existing);

        updateSameProfile(existing, UserRole.EDITOR, false);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: []; can_approve: true → false"));
    }

    @Test
    void update_keepsCanApprove_whenFieldOmitted() {
        User existing = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(existing);

        UpdateUserResponseDto response = updateSameProfile(existing, UserRole.EDITOR, null);

        assertThat(response.canApprove()).isTrue();
        assertThat(existing.isCanApprove()).isTrue();
    }

    @Test
    void update_doesNotLogCanApprove_whenValueUnchanged() {
        User existing = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(existing);

        updateSameProfile(existing, UserRole.EDITOR, true);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: []"));
    }

    @Test
    void update_throwsBusinessRule_whenReaderStaysReaderWithCanApproveTrue() {
        User reader = TestFixtures.userReader(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(reader));

        assertThatThrownBy(() -> updateSameProfile(reader, UserRole.READER, true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(READER_APPROVER_KEY)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        assertThat(reader.isCanApprove()).isFalse();
        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }
    // ------------------------------------------------------------------
    // changeStatus()
    // ------------------------------------------------------------------

    @Test
    void changeStatus_sameStatus_throwsBusinessRule400() {
        User existing = TestFixtures.userEditor(30L); // ACTIVE
        UpdateStatusRequestDto request = TestFixtures.updateStatusRequest(UserStatus.ACTIVE);

        when(userRepository.findById(30L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.changeStatus(30L, request, AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.INVALID_ARGUMENT));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void changeStatus_adminDeactivatingSelf_throwsBusinessRule403() {
        User selfAdmin = TestFixtures.userAdmin(ADMIN_ID); // ACTIVE
        UpdateStatusRequestDto request = TestFixtures.updateStatusRequest(UserStatus.INACTIVE);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() -> userService.changeStatus(ADMIN_ID, request, AUDIT_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(BusinessErrorCode.FORBIDDEN));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    @Test
    void changeStatus_activateInactiveUser_persists_logsDeactivateUser_returnsActivatedMessage() {
        User inactive = TestFixtures.userInactive(40L);
        UpdateStatusRequestDto request = TestFixtures.updateStatusRequest(UserStatus.ACTIVE);

        when(userRepository.findById(40L)).thenReturn(Optional.of(inactive));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStatusResponseDto response = userService.changeStatus(40L, request, AUDIT_ADMIN);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.message()).isEqualTo("user.activated.success");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(activityLogService).record(
                eq(ActivityAction.ACTIVATE_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Nuevo estado: ACTIVE"));
    }

    @Test
    void changeStatus_deactivateActiveUser_persists_returnsDeactivatedMessage() {
        User active = TestFixtures.userEditor(41L);
        UpdateStatusRequestDto request = TestFixtures.updateStatusRequest(UserStatus.INACTIVE);

        when(userRepository.findById(41L)).thenReturn(Optional.of(active));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStatusResponseDto response = userService.changeStatus(41L, request, AUDIT_ADMIN);

        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.message()).isEqualTo("user.deactivated.success");

        verify(activityLogService).record(
                eq(ActivityAction.DEACTIVATE_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Nuevo estado: INACTIVE"));
    }

    @Test
    void changeStatus_missingUser_throwsResourceNotFound() {
        UpdateStatusRequestDto request = TestFixtures.updateStatusRequest(UserStatus.INACTIVE);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeStatus(404L, request, AUDIT_ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(activityLogService);
    }

    // ------------------------------------------------------------------
    // canApprove (HU-32): US3 retiro automatico al pasar a lector
    // ------------------------------------------------------------------

    @Test
    void update_removesCanApprove_whenRoleChangesToReader() {
        User approver = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(approver);

        UpdateUserResponseDto response = updateSameProfile(approver, UserRole.READER, null);

        assertThat(response.role()).isEqualTo("READER");
        assertThat(response.canApprove()).isFalse();
        assertThat(approver.isCanApprove()).isFalse();
    }

    @Test
    void update_removesCanApproveWithoutError_whenRoleChangesToReaderAndFlagTrueSent() {
        User approver = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(approver);

        UpdateUserResponseDto response = updateSameProfile(approver, UserRole.READER, true);

        assertThat(response.canApprove()).isFalse();
        assertThat(approver.isCanApprove()).isFalse();
    }

    @Test
    void update_logsCanApproveTrueToFalse_whenRoleChangesToReader() {
        User approver = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(approver);

        updateSameProfile(approver, UserRole.READER, null);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: [role]; can_approve: true → false"));
    }

    @Test
    void update_doesNotLogCanApprove_whenReaderAlreadyFalseAndFieldOmitted() {
        User reader = TestFixtures.userReader(20L);
        stubExistingUserForUpdate(reader);

        updateSameProfile(reader, UserRole.READER, null);

        verify(activityLogService).record(
                eq(ActivityAction.EDIT_USER),
                eq(AUDIT_ADMIN),
                isNull(),
                eq("Campos modificados: []"));
    }

    // ------------------------------------------------------------------
    // canApprove (HU-32): US4 exposicion en respuestas
    // ------------------------------------------------------------------

    @Test
    void list_exposesCanApprove_forApproverAndNonApprover() {
        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(TestFixtures.userApprover(2L), TestFixtures.userEditor(3L)));

        UserListResponseDto response = userService.list(null, null);

        assertThat(response.users())
                .extracting(UserResponseDto::canApprove)
                .containsExactly(true, false);
    }

    // ------------------------------------------------------------------
    // canApprove (HU-32): US5 aprobador activo y ciclo de vida
    // ------------------------------------------------------------------

    @Test
    void isActiveApprover_returnsTrue_whenPermissionActiveAndUserActiveAndNotReader() {
        when(userRepository.existsByIdAndCanApproveTrueAndStatusAndRoleNot(
                30L, UserStatus.ACTIVE, UserRole.READER)).thenReturn(true);

        assertThat(userService.isActiveApprover(30L)).isTrue();
    }

    @Test
    void isActiveApprover_returnsFalse_whenRepositoryReportsNoMatch() {
        when(userRepository.existsByIdAndCanApproveTrueAndStatusAndRoleNot(
                30L, UserStatus.ACTIVE, UserRole.READER)).thenReturn(false);

        assertThat(userService.isActiveApprover(30L)).isFalse();
    }

    @Test
    void isActiveApprover_queriesRepositoryOnEveryCall() {
        when(userRepository.existsByIdAndCanApproveTrueAndStatusAndRoleNot(
                30L, UserStatus.ACTIVE, UserRole.READER)).thenReturn(true, false);

        assertThat(userService.isActiveApprover(30L)).isTrue();
        assertThat(userService.isActiveApprover(30L)).isFalse();

        verify(userRepository, times(2)).existsByIdAndCanApproveTrueAndStatusAndRoleNot(
                30L, UserStatus.ACTIVE, UserRole.READER);
    }

    @Test
    void changeStatus_keepsCanApprove_whenDeactivatedAndReactivated() {
        User approver = TestFixtures.userApprover(41L);
        when(userRepository.findById(41L)).thenReturn(Optional.of(approver));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changeStatus(41L, TestFixtures.updateStatusRequest(UserStatus.INACTIVE), AUDIT_ADMIN);
        assertThat(approver.isCanApprove()).isTrue();

        userService.changeStatus(41L, TestFixtures.updateStatusRequest(UserStatus.ACTIVE), AUDIT_ADMIN);
        assertThat(approver.isCanApprove()).isTrue();
    }

    @Test
    void update_isActiveApproverFalseImmediately_afterPermissionRemoved() {
        User approver = TestFixtures.userApprover(20L);
        stubExistingUserForUpdate(approver);
        when(userRepository.existsByIdAndCanApproveTrueAndStatusAndRoleNot(
                20L, UserStatus.ACTIVE, UserRole.READER))
                .thenAnswer(inv -> approver.isCanApprove());

        assertThat(userService.isActiveApprover(20L)).isTrue();

        updateSameProfile(approver, UserRole.EDITOR, false);

        assertThat(userService.isActiveApprover(20L)).isFalse();
    }
}
