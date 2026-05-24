package co.edu.docurural.auth.service;

import co.edu.docurural.auth.dto.LoginRequestDto;
import co.edu.docurural.auth.dto.LoginResponseDto;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.dto.MessageResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request, AuditContext audit);

    MessageResponseDto logout(AuditContext audit);
}
