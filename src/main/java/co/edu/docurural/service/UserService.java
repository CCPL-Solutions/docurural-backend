package co.edu.docurural.service;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.ActivityAction;
import co.edu.docurural.domain.enums.enums.UserStatus;
import co.edu.docurural.domain.repository.UserRepository;
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
import co.edu.docurural.web.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio de gestion de usuarios para los endpoints {@code USR-01..USR-05}.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Orquestar el CRUD de usuarios respetando las reglas del contrato
 *       {@code docs/api-rest-sprint1.md} (unicidad de email, auto-proteccion
 *       del administrador sobre su propio rol y estado).</li>
 *   <li>Hashear contrasenas con {@link PasswordEncoder} antes de persistir.</li>
 *   <li>Registrar las acciones {@code CREATE_USER}, {@code EDIT_USER} y
 *       {@code DEACTIVATE_USER} en {@code activity_log} con el formato del
 *       contrato.</li>
 *   <li>Mapear entidad -> DTO exclusivamente a traves de {@link UserMapper}
 *       para garantizar que {@code passwordHash} jamas se expone.</li>
 * </ul>
 *
 * <p>Todas las excepciones de negocio se propagan hacia el
 * {@code GlobalExceptionHandler} (Fase 7) que las traduce al formato estandar
 * de error.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String USER_CREATED_MESSAGE = "Usuario creado exitosamente";
    private static final String USER_UPDATED_MESSAGE = "Usuario actualizado exitosamente";
    private static final String USER_ACTIVATED_MESSAGE = "Usuario activado exitosamente";
    private static final String USER_DEACTIVATED_MESSAGE = "Usuario desactivado exitosamente";

    private static final String EMAIL_ALREADY_REGISTERED_MESSAGE =
            "Ya existe un usuario registrado con este correo electronico";
    private static final String SELF_ROLE_CHANGE_MESSAGE =
            "No puede cambiar su propio rol";
    private static final String SELF_DEACTIVATION_MESSAGE =
            "No puede desactivar su propia cuenta";
    private static final String DUPLICATE_STATUS_MESSAGE =
            "El usuario ya se encuentra en el estado solicitado";
    private static final String PASSWORDS_MISMATCH_MESSAGE =
            "Las contrasenas no coinciden";
    private static final String UNSUPPORTED_SORT_FIELD_MESSAGE =
            "Campo de ordenamiento no soportado: ";
    private static final String UNSUPPORTED_SORT_DIRECTION_MESSAGE =
            "Direccion de ordenamiento no soportada: ";

    private static final String DEFAULT_SORT_BY = "fullName";
    private static final String DEFAULT_SORT_DIR = "asc";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "fullName", "email", "role", "status", "createdAt", "lastLogin");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    /**
     * Retorna el listado completo de usuarios ordenado segun los parametros.
     *
     * @param sortBy  campo de ordenamiento (default {@code fullName}).
     *                Debe pertenecer a {@link #ALLOWED_SORT_FIELDS}.
     * @param sortDir direccion {@code asc} o {@code desc} (default {@code asc},
     *                case-insensitive).
     * @return {@link UserListResponse} con el total y la lista ya mapeada.
     * @throws BusinessRuleException {@code 400} si {@code sortBy} o
     *                               {@code sortDir} son invalidos.
     */
    @Transactional(readOnly = true)
    public UserListResponse list(String sortBy, String sortDir) {
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? DEFAULT_SORT_BY : sortBy;
        String resolvedSortDir = (sortDir == null || sortDir.isBlank()) ? DEFAULT_SORT_DIR : sortDir;

        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new BusinessRuleException(
                    HttpStatus.BAD_REQUEST,
                    UNSUPPORTED_SORT_FIELD_MESSAGE + resolvedSortBy);
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(resolvedSortDir.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(
                    HttpStatus.BAD_REQUEST,
                    UNSUPPORTED_SORT_DIRECTION_MESSAGE + resolvedSortDir);
        }

        List<User> users = userRepository.findAll(Sort.by(direction, resolvedSortBy));
        log.debug("Listado de usuarios: total={} sortBy={} sortDir={}",
                users.size(), resolvedSortBy, direction);
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
                        "Usuario no encontrado con id " + id));
        return UserMapper.toResponse(user);
    }

    /**
     * Crea un nuevo usuario con {@code status = ACTIVE} y hashea la contrasena
     * con BCrypt.
     *
     * <p>Reglas:
     * <ul>
     *   <li>Password y confirmacion deben coincidir (defensa redundante; la
     *       anotacion {@code @PasswordsMatch} ya se ejecuto a nivel de DTO).</li>
     *   <li>El email debe ser unico entre usuarios activos e inactivos.</li>
     *   <li>Al finalizar se registra la accion {@code CREATE_USER} con detalle
     *       {@code "Usuario creado: {id}"} (contrato USR-03).</li>
     * </ul>
     *
     * @throws BusinessRuleException {@code 400} si las contrasenas no coinciden.
     * @throws ConflictException     {@code 409} si el email ya esta registrado.
     */
    @Transactional
    public CreateUserResponse create(CreateUserRequest request, Long adminId, HttpServletRequest httpRequest) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessRuleException(HttpStatus.BAD_REQUEST, PASSWORDS_MISMATCH_MESSAGE);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(EMAIL_ALREADY_REGISTERED_MESSAGE);
        }

        User newUser = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);

        activityLogService.record(
                ActivityAction.CREATE_USER,
                adminId,
                null,
                "Usuario creado: " + savedUser.getId(),
                httpRequest);

        log.info("Usuario creado: id={} email={} role={} por adminId={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole(), adminId);

        return UserMapper.toCreateResponse(savedUser, USER_CREATED_MESSAGE);
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * <p>Reglas:
     * <ul>
     *   <li>Si el email cambia, debe ser unico entre los demas usuarios.</li>
     *   <li>El administrador no puede cambiar su propio rol.</li>
     *   <li>Si se envia {@code password}, debe coincidir con
     *       {@code confirmPassword} y se rehashea; si se omite o llega vacio,
     *       la contrasena actual se preserva.</li>
     *   <li>El detalle del log lista los campos efectivamente modificados en el
     *       formato {@code "Campos modificados: [field1, field2, ...]"}
     *       (contrato USR-04).</li>
     * </ul>
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     * @throws ConflictException         {@code 409} si el nuevo email ya pertenece a otro usuario.
     * @throws BusinessRuleException     {@code 400} si las contrasenas no coinciden,
     *                                   o {@code 403} si intenta cambiar su propio rol.
     */
    @Transactional
    public UpdateUserResponse update(
            Long id, UpdateUserRequest request, Long adminId, HttpServletRequest httpRequest) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id " + id));

        boolean emailChanged = !request.email().equalsIgnoreCase(user.getEmail());
        if (emailChanged && userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException(EMAIL_ALREADY_REGISTERED_MESSAGE);
        }

        boolean roleChanged = request.role() != user.getRole();
        if (roleChanged && id.equals(adminId)) {
            throw new BusinessRuleException(HttpStatus.FORBIDDEN, SELF_ROLE_CHANGE_MESSAGE);
        }

        boolean passwordProvided = request.password() != null && !request.password().isBlank();
        if (passwordProvided && !request.password().equals(request.confirmPassword())) {
            throw new BusinessRuleException(HttpStatus.BAD_REQUEST, PASSWORDS_MISMATCH_MESSAGE);
        }

        List<String> modifiedFields = new ArrayList<>();

        if (!request.fullName().equals(user.getFullName())) {
            user.setFullName(request.fullName());
            modifiedFields.add("fullName");
        }
        if (emailChanged) {
            user.setEmail(request.email());
            modifiedFields.add("email");
        }
        if (roleChanged) {
            user.setRole(request.role());
            modifiedFields.add("role");
        }
        if (passwordProvided) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            modifiedFields.add("password");
        }

        User updatedUser = userRepository.save(user);

        String detail = "Campos modificados: " + modifiedFields;
        activityLogService.record(
                ActivityAction.EDIT_USER,
                adminId,
                null,
                detail,
                httpRequest);

        log.info("Usuario actualizado: id={} modifiedFields={} por adminId={}",
                updatedUser.getId(), modifiedFields, adminId);

        return UserMapper.toUpdateResponse(updatedUser, USER_UPDATED_MESSAGE);
    }

    /**
     * Cambia el estado ({@code ACTIVE}/{@code INACTIVE}) de un usuario.
     *
     * <p>Reglas:
     * <ul>
     *   <li>El nuevo estado no puede ser igual al actual.</li>
     *   <li>El administrador no puede desactivar su propia cuenta.</li>
     *   <li>Se registra la accion {@code DEACTIVATE_USER} incluso al reactivar:
     *       el enum reutiliza el mismo valor (plan Fase 5 - nota del punto 3.3).
     *       El detalle incluye el nuevo estado aplicado.</li>
     * </ul>
     *
     * @throws ResourceNotFoundException {@code 404} si el id no existe.
     * @throws BusinessRuleException     {@code 400} si el usuario ya tiene el estado
     *                                   solicitado, o {@code 403} si el admin intenta
     *                                   desactivarse a si mismo.
     */
    @Transactional
    public UpdateStatusResponse changeStatus(
            Long id, UpdateStatusRequest request, Long adminId, HttpServletRequest httpRequest) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id " + id));

        UserStatus newStatus = request.status();

        if (user.getStatus() == newStatus) {
            throw new BusinessRuleException(HttpStatus.BAD_REQUEST, DUPLICATE_STATUS_MESSAGE);
        }

        if (id.equals(adminId) && newStatus == UserStatus.INACTIVE) {
            throw new BusinessRuleException(HttpStatus.FORBIDDEN, SELF_DEACTIVATION_MESSAGE);
        }

        user.setStatus(newStatus);
        User updatedUser = userRepository.save(user);

        String message = newStatus == UserStatus.ACTIVE
                ? USER_ACTIVATED_MESSAGE
                : USER_DEACTIVATED_MESSAGE;

        activityLogService.record(
                ActivityAction.DEACTIVATE_USER,
                adminId,
                null,
                "Nuevo estado: " + newStatus.name(),
                httpRequest);

        log.info("Estado de usuario actualizado: id={} newStatus={} por adminId={}",
                updatedUser.getId(), newStatus, adminId);

        return UserMapper.toStatusResponse(updatedUser, message);
    }
}
