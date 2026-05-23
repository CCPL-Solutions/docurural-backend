package co.edu.docurural.user.service;

import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.user.dto.CreateUserRequest;
import co.edu.docurural.user.dto.CreateUserResponse;
import co.edu.docurural.user.dto.UpdateStatusRequest;
import co.edu.docurural.user.dto.UpdateStatusResponse;
import co.edu.docurural.user.dto.UpdateUserRequest;
import co.edu.docurural.user.dto.UpdateUserResponse;
import co.edu.docurural.user.dto.UserListResponse;
import co.edu.docurural.user.dto.UserResponse;

public interface UserService {

    UserListResponse list(String sortBy, String sortDir);

    UserResponse findById(Long id);

    CreateUserResponse create(CreateUserRequest request, AuditContext audit);

    UpdateUserResponse update(Long id, UpdateUserRequest request, AuditContext audit);

    UpdateStatusResponse changeStatus(Long id, UpdateStatusRequest request, AuditContext audit);
}
