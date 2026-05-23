package co.edu.docurural.auth.service;

import co.edu.docurural.auth.dto.LoginRequest;
import co.edu.docurural.auth.dto.LoginResponse;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.dto.MessageResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, AuditContext audit);

    MessageResponse logout(AuditContext audit);
}
