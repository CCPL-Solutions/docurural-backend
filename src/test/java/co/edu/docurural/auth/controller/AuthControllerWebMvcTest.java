package co.edu.docurural.auth.controller;

import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import co.edu.docurural.shared.security.SecurityConfig;
import co.edu.docurural.auth.service.AuthService;
import co.edu.docurural.auth.dto.LoginRequest;
import co.edu.docurural.auth.dto.LoginResponse;
import co.edu.docurural.auth.dto.UserSummary;
import co.edu.docurural.shared.dto.MessageResponse;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @Test
    void login_withValidBody_returns200AndResponse() throws Exception {
        LoginResponse response = LoginResponse.bearer(
                "token-abc",
                1800L,
                new UserSummary(10L, "Ana Admin", "ana.admin@docurural.edu.co", "ADMIN"));

        when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new LoginRequest(
                "ana.admin@docurural.edu.co",
                "plain-password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.user.id").value(10))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        verify(authService).login(
                argThat(request -> "ana.admin@docurural.edu.co".equals(request.email())),
                any(HttpServletRequest.class));
    }

    @Test
    void login_withInvalidBody_returns400WithValidationErrors() throws Exception {
        String body = """
                {
                  "email": "",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_whenServiceThrowsBadCredentials_returns401MappedError() throws Exception {
        when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String body = objectMapper.writeValueAsString(new LoginRequest(
                "ana.admin@docurural.edu.co",
                "wrong-password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void logout_returns200() throws Exception {
        when(authService.logout(any(HttpServletRequest.class)))
                .thenReturn(new MessageResponse("Sesión cerrada exitosamente"));

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sesión cerrada exitosamente"));
    }
}
