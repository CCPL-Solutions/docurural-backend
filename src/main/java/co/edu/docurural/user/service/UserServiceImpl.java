package co.edu.docurural.user.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.dto.CreateUserResponseDto;
import co.edu.docurural.user.dto.UserResponseDto;
import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserStatus;
import co.edu.docurural.user.repository.UserRepository;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.util.FieldUpdater;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.shared.util.SortingValidator;
import co.edu.docurural.user.dto.CreateUserRequestDto;
import co.edu.docurural.user.dto.UpdateStatusRequestDto;
import co.edu.docurural.user.dto.UpdateStatusResponseDto;
import co.edu.docurural.user.dto.UpdateUserRequestDto;
import co.edu.docurural.user.dto.UpdateUserResponseDto;
import co.edu.docurural.user.dto.UserListResponseDto;
import co.edu.docurural.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_SORT_BY = "fullName";
    private static final String DEFAULT_SORT_DIR = "asc";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "fullName", "email", "role", "status", "createdAt", "lastLogin");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;
    private final SortingValidator sortingValidator;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserListResponseDto list(String sortBy, String sortDir) {
        Sort sort = sortingValidator.resolveSort(
                sortBy, sortDir,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_BY, DEFAULT_SORT_DIR,
                "user.sort.unsupported-field", "user.sort.unsupported-direction");

        List<User> users = userRepository.findAll(sort);
        log.debug("Listado de usuarios: total={} sortBy={} sortDir={}",
                users.size(), sortBy, sortDir);
        return userMapper.toListResponse(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("user.not-found", id)));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public CreateUserResponseDto create(CreateUserRequestDto request, AuditContext audit) {
        Long adminId = requireActorUserId(audit);

        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, messageResolver.get("user.passwords.mismatch"));
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException(messageResolver.get("user.email.already-registered"));
        }

        User newUser = User.builder()
                .fullName(request.fullName())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);

        activityLogService.record(
                ActivityAction.CREATE_USER,
                audit,
                null,
                "Usuario creado: " + savedUser.getId());

        log.info("Usuario creado: id={} email={} role={} por adminId={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole(), adminId);

        return userMapper.toCreateResponse(savedUser, messageResolver.get("user.created.success"));
    }

    @Override
    @Transactional
    public UpdateUserResponseDto update(Long id, UpdateUserRequestDto request, AuditContext audit) {
        Long adminId = requireActorUserId(audit);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("user.not-found", id)));

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        validateEmailUniqueness(normalizedEmail, id, user);
        validateRoleChange(request.role(), user, id, adminId);
        validatePasswordConsistency(request.password(), request.confirmPassword());

        List<String> modifiedFields = applyUpdates(user, request, normalizedEmail);

        User updatedUser = userRepository.save(user);

        recorderUpdateLog(audit, modifiedFields);

        log.info("Usuario actualizado: id={} modifiedFields={} por adminId={}",
                updatedUser.getId(), modifiedFields, adminId);

        return userMapper.toUpdateResponse(updatedUser, messageResolver.get("user.updated.success"));
    }

    @Override
    @Transactional
    public UpdateStatusResponseDto changeStatus(Long id, UpdateStatusRequestDto request, AuditContext audit) {
        Long adminId = requireActorUserId(audit);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("user.not-found", id)));

        UserStatus newStatus = request.status();

        if (user.getStatus() == newStatus) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, messageResolver.get("user.status.duplicate"));
        }

        if (id.equals(adminId) && newStatus == UserStatus.INACTIVE) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN, messageResolver.get("user.self-deactivation.forbidden"));
        }

        user.setStatus(newStatus);
        if (newStatus == UserStatus.INACTIVE) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        }
        User updatedUser = userRepository.save(user);

        String message = newStatus == UserStatus.ACTIVE
                ? messageResolver.get("user.activated.success")
                : messageResolver.get("user.deactivated.success");

        activityLogService.record(
                ActivityAction.DEACTIVATE_USER,
                audit,
                null,
                "Nuevo estado: " + newStatus.name());

        log.info("Estado de usuario actualizado: id={} newStatus={} por adminId={}",
                updatedUser.getId(), newStatus, adminId);

        return userMapper.toStatusResponse(updatedUser, message);
    }

    private void validateEmailUniqueness(String newEmail, Long userId, User currentUser) {
        boolean emailChanged = !newEmail.equalsIgnoreCase(currentUser.getEmail());
        if (emailChanged && userRepository.existsByEmailAndIdNot(newEmail, userId)) {
            throw new ConflictException(messageResolver.get("user.email.already-registered"));
        }
    }

    private void validateRoleChange(Object newRole, User currentUser, Long userId, Long adminId) {
        boolean roleChanged = newRole != currentUser.getRole();
        if (roleChanged && userId.equals(adminId)) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN, messageResolver.get("user.self-role-change.forbidden"));
        }
    }

    private void validatePasswordConsistency(String password, String confirmPassword) {
        boolean passwordProvided = password != null && !password.isBlank();
        if (passwordProvided && !password.equals(confirmPassword)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, messageResolver.get("user.passwords.mismatch"));
        }
    }

    private List<String> applyUpdates(User user, UpdateUserRequestDto request, String normalizedEmail) {
        List<String> modifiedFields = new ArrayList<>(
                FieldUpdater.of(user)
                        .setIfChanged("fullName", request.fullName(), user::getFullName, user::setFullName)
                        .setIfChanged("email", normalizedEmail, user::getEmail, user::setEmail)
                        .changedFields());

        if (request.role() != user.getRole()) {
            user.setRole(request.role());
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            modifiedFields.add("role");
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            modifiedFields.add("password");
        }

        return modifiedFields;
    }

    private void recorderUpdateLog(AuditContext audit, List<String> modifiedFields) {
        activityLogService.record(
                ActivityAction.EDIT_USER,
                audit,
                null,
                "Campos modificados: " + modifiedFields);
    }

    private Long requireActorUserId(AuditContext audit) {
        if (audit == null) {
            throw new IllegalArgumentException("audit no puede ser null");
        }
        if (audit.actorUserId() == null) {
            throw new IllegalArgumentException("audit.actorUserId no puede ser null");
        }
        return audit.actorUserId();
    }
}
