package co.edu.docurural.user.service;

import co.edu.docurural.activitylog.enums.ActivityAction;
import co.edu.docurural.activitylog.service.ActivityLogService;
import co.edu.docurural.shared.audit.AuditContext;
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
import co.edu.docurural.user.dto.CreateUserRequest;
import co.edu.docurural.user.dto.CreateUserResponse;
import co.edu.docurural.user.dto.UpdateStatusRequest;
import co.edu.docurural.user.dto.UpdateStatusResponse;
import co.edu.docurural.user.dto.UpdateUserRequest;
import co.edu.docurural.user.dto.UpdateUserResponse;
import co.edu.docurural.user.dto.UserListResponse;
import co.edu.docurural.user.dto.UserResponse;
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

/**
 * Servicio de gestión de usuarios para los endpoints {@code USR-01..USR-05}.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Orquestar el CRUD de usuarios respetando las reglas del contrato
 *       {@code docs/api-rest-sprint1.md} (unicidad de email, auto-protección
 *       del administrador sobre su propio rol y estado).</li>
 *   <li>Hashear contraseñas con {@link PasswordEncoder} antes de persistir.</li>
 *   <li>Registrar las acciones {@code CREATE_USER}, {@code EDIT_USER} y
 *       {@code DEACTIVATE_USER} en {@code activity_log} con el formato del
 *       contrato.</li>
 *   <li>Mapear entidad -> DTO exclusivamente a través de {@link UserMapper}
 *       para garantizar que {@code passwordHash} jamás se expone.</li>
 * </ul>
 *
 * <p>Todas las excepciones de negocio se propagan hacia el
 * {@code GlobalExceptionHandler} (Fase 7) que las traduce al formato estándar
 * de error.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String DEFAULT_SORT_BY = "fullName";
    private static final String DEFAULT_SORT_DIR = "asc";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "fullName", "email", "role", "status", "createdAt", "lastLogin");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final MessageResolver messageResolver;

    /**
     * Retorna el listado completo de usuarios ordenado según los parámetros.
     *
     * @param sortBy  campo de ordenamiento (default {@code fullName}).
     *                Debe pertenecer a {@link #ALLOWED_SORT_FIELDS}.
     * @param sortDir dirección {@code asc} o {@code desc} (default {@code asc},
     *                case-insensitive).
     * @return {@link UserListResponse} con el total y la lista ya mapeada.
     * @throws BusinessRuleException {@code 400} si {@code sortBy} o
     *                               {@code sortDir} son inválidos.
     */
    @Transactional(readOnly = true)
    public UserListResponse list(String sortBy, String sortDir) {
        Sort sort = SortingValidator.resolveSort(
                sortBy, sortDir,
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_BY, DEFAULT_SORT_DIR,
                messageResolver.get("user.sort.unsupported-field", sortBy),
                messageResolver.get("user.sort.unsupported-direction", sortDir));

        List<User> users = userRepository.findAll(sort);
        log.debug("Listado de usuarios: total={} sortBy={} sortDir={}",
                users.size(), sortBy, sortDir);
        return UserMapper.toListResponse(users);
    }

    /**
     * Recupera un usuario por id.
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     */
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("user.not-found", id)));
        return UserMapper.toResponse(user);
    }

    /**
     * Crea un nuevo usuario con {@code status = ACTIVE} y hashea la contraseña
     * con BCrypt.
     *
     * <p>Reglas:
     * <ul>
     *   <li>Password y confirmación deben coincidir (defensa redundante; la
     *       anotación {@code @PasswordsMatch} ya se ejecutó a nivel de DTO).</li>
     *   <li>El email debe ser único entre usuarios activos e inactivos.</li>
     *   <li>Al finalizar se registra la acción {@code CREATE_USER} con detalle
     *       {@code "Usuario creado: {id}"} (contrato USR-03).</li>
     * </ul>
     *
     * @throws BusinessRuleException {@code 400} si las contraseñas no coinciden.
     * @throws ConflictException     {@code 409} si el email ya está registrado.
     */
    @Transactional
    public CreateUserResponse create(CreateUserRequest request, AuditContext audit) {
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

        return UserMapper.toCreateResponse(savedUser, messageResolver.get("user.created.success"));
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * <p>Reglas:
     * <ul>
     *   <li>Si el email cambia, debe ser único entre los demás usuarios.</li>
     *   <li>El administrador no puede cambiar su propio rol.</li>
     *   <li>Si se envía {@code password}, debe coincidir con
     *       {@code confirmPassword} y se rehashea; si se omite o llega vacío,
     *       la contraseña actual se preserva.</li>
     *   <li>El detalle del log lista los campos efectivamente modificados en el
     *       formato {@code "Campos modificados: [field1, field2, ...]"}
     *       (contrato USR-04).</li>
     * </ul>
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     * @throws ConflictException         {@code 409} si el nuevo email ya pertenece a otro usuario.
     * @throws BusinessRuleException     {@code 400} si las contraseñas no coinciden,
     *                                   o {@code 403} si intenta cambiar su propio rol.
     */
    @Transactional
    public UpdateUserResponse update(
            Long id, UpdateUserRequest request, AuditContext audit) {

        Long adminId = requireActorUserId(audit);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageResolver.get("user.not-found", id)));

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        // Realizar validaciones
        validateEmailUniqueness(normalizedEmail, id, user);
        validateRoleChange(request.role(), user, id, adminId);
        validatePasswordConsistency(request.password(), request.confirmPassword());

        // Actualizar campos y registrar cambios
        List<String> modifiedFields = applyUpdates(user, request, normalizedEmail);

        User updatedUser = userRepository.save(user);

        recorderUpdateLog(audit, modifiedFields);

        log.info("Usuario actualizado: id={} modifiedFields={} por adminId={}",
                updatedUser.getId(), modifiedFields, adminId);

        return UserMapper.toUpdateResponse(updatedUser, messageResolver.get("user.updated.success"));
    }

    /**
     * Valida que el email sea único si ha cambiado.
     *
     * @throws ConflictException si el email ya existe en otro usuario.
     */
    private void validateEmailUniqueness(String newEmail, Long userId, User currentUser) {
        boolean emailChanged = !newEmail.equalsIgnoreCase(currentUser.getEmail());
        if (emailChanged && userRepository.existsByEmailAndIdNot(newEmail, userId)) {
            throw new ConflictException(messageResolver.get("user.email.already-registered"));
        }
    }

    /**
     * Valida que el administrador no intente cambiar su propio rol.
     *
     * @throws BusinessRuleException si el admin intenta cambiar su propio rol.
     */
    private void validateRoleChange(Object newRole, User currentUser, Long userId, Long adminId) {
        boolean roleChanged = newRole != currentUser.getRole();
        if (roleChanged && userId.equals(adminId)) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN, messageResolver.get("user.self-role-change.forbidden"));
        }
    }

    /**
     * Valida que la contraseña y su confirmación coincidan.
     *
     * @throws BusinessRuleException si las contraseñas no coinciden.
     */
    private void validatePasswordConsistency(String password, String confirmPassword) {
        boolean passwordProvided = password != null && !password.isBlank();
        if (passwordProvided && !password.equals(confirmPassword)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, messageResolver.get("user.passwords.mismatch"));
        }
    }

    /**
     * Aplica los cambios al usuario y retorna la lista de campos modificados.
     *
     * @return Lista con los nombres de los campos que fueron actualizados.
     */
    private List<String> applyUpdates(User user, UpdateUserRequest request, String normalizedEmail) {
        List<String> modifiedFields = new ArrayList<>(
                FieldUpdater.of(user)
                        .setIfChanged("fullName", request.fullName(), user::getFullName, user::setFullName)
                        .setIfChanged("email", normalizedEmail, user::getEmail, user::setEmail)
                        .changedFields());

        if (request.role() != user.getRole()) {
            user.setRole(request.role());
            // el cambio de rol invalida todos los tokens emitidos con el rol anterior
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            modifiedFields.add("role");
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            modifiedFields.add("password");
        }

        return modifiedFields;
    }

    /**
     * Registra la actualización del usuario en el log de actividades.
     */
    private void recorderUpdateLog(AuditContext audit,
                                   List<String> modifiedFields) {
        String detail = "Campos modificados: " + modifiedFields;
        activityLogService.record(
                ActivityAction.EDIT_USER,
                audit,
                null,
                detail);
    }

    /**
     * Cambia el estado ({@code ACTIVE}/{@code INACTIVE}) de un usuario.
     *
     * <p>Reglas:
     * <ul>
     *   <li>El nuevo estado no puede ser igual al actual.</li>
     *   <li>El administrador no puede desactivar su propia cuenta.</li>
     *   <li>Se registra la acción {@code DEACTIVATE_USER} incluso al reactivar:
     *       el enum reutiliza el mismo valor (plan Fase 5 - nota del punto 3.3).
     *       El detalle incluye el nuevo estado aplicado.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     * @throws BusinessRuleException     {@code 400} si el usuario ya tiene el estado
     *                                   solicitado, o {@code 403} si el admin intenta
     *                                   desactivarse a sí mismo.
     */
    @Transactional
    public UpdateStatusResponse changeStatus(
            Long id, UpdateStatusRequest request, AuditContext audit) {

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
        // al desactivar, invalida todos los tokens activos del usuario
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

        return UserMapper.toStatusResponse(updatedUser, message);
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
