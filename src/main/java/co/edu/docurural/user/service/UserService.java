package co.edu.docurural.user.service;

import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.dto.CreateUserRequestDto;
import co.edu.docurural.user.dto.CreateUserResponseDto;
import co.edu.docurural.user.dto.UpdateStatusRequestDto;
import co.edu.docurural.user.dto.UpdateStatusResponseDto;
import co.edu.docurural.user.dto.UpdateUserRequestDto;
import co.edu.docurural.user.dto.UpdateUserResponseDto;
import co.edu.docurural.user.dto.UserListResponseDto;
import co.edu.docurural.user.dto.UserResponseDto;

public interface UserService {

    UserListResponseDto list(String sortBy, String sortDir);

    UserResponseDto findById(Long id);

    CreateUserResponseDto create(CreateUserRequestDto request, AuditContext audit);

    UpdateUserResponseDto update(Long id, UpdateUserRequestDto request, AuditContext audit);

    UpdateStatusResponseDto changeStatus(Long id, UpdateStatusRequestDto request, AuditContext audit);

    /**
     * Indica si el usuario puede aprobar documentos en este momento.
     *
     * <p>Es {@code true} solo si tiene el permiso activo, su estado es {@code ACTIVE} y su rol no
     * es {@code READER}. Un usuario inexistente devuelve {@code false}. Consulta la base de datos
     * en cada llamada: los consumidores deben invocarlo en cada operación, sin cachear el
     * resultado ni derivarlo del token.
     */
    boolean isActiveApprover(Long userId);
}
