package co.edu.docurural.web.controller;

import co.edu.docurural.config.security.CustomUserPrincipal;
import co.edu.docurural.config.security.JwtAuthenticationFilter;
import co.edu.docurural.config.security.SecurityConfig;
import co.edu.docurural.domain.enums.UserRole;
import co.edu.docurural.domain.enums.UserStatus;
import co.edu.docurural.service.UserService;
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
import co.edu.docurural.web.exception.GlobalExceptionHandler;
import co.edu.docurural.web.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_returns200AndPayload() throws Exception {
        UserResponse user = new UserResponse(
                2L,
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                "EDITOR",
                "ACTIVE",
                LocalDateTime.of(2026, 1, 1, 8, 0),
                null);
        when(userService.list("fullName", "asc")).thenReturn(new UserListResponse(1, List.of(user)));

        mockMvc.perform(get("/users")
                        .param("sortBy", "fullName")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.users[0].id").value(2))
                .andExpect(jsonPath("$.users[0].role").value("EDITOR"));

        verify(userService).list("fullName", "asc");
    }

    @Test
    void list_withoutParams_delegatesNullsToService() throws Exception {
        when(userService.list(null, null)).thenReturn(new UserListResponse(0, List.of()));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0))
                .andExpect(jsonPath("$.users").isArray());

        verify(userService).list(null, null);
    }

    @Test
    void list_withInvalidSortField_returns400MappedError() throws Exception {
        when(userService.list("invalido", "asc"))
                .thenThrow(new BusinessRuleException(
                        HttpStatus.BAD_REQUEST,
                        "El campo de ordenamiento 'invalido' no es soportado"));

        mockMvc.perform(get("/users")
                        .param("sortBy", "invalido")
                        .param("sortDir", "asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El campo de ordenamiento 'invalido' no es soportado"));
    }

    @Test
    void getById_returns200AndPayload() throws Exception {
        UserResponse user = new UserResponse(
                2L,
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                "EDITOR",
                "ACTIVE",
                LocalDateTime.of(2026, 1, 1, 8, 0),
                LocalDateTime.of(2026, 2, 1, 10, 30));

        when(userService.findById(2L)).thenReturn(user);

        mockMvc.perform(get("/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.fullName").value("Erik Editor"))
                .andExpect(jsonPath("$.email").value("erik.editor@docurural.edu.co"))
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).findById(2L);
    }

    @Test
    void getById_whenNotFound_returns404MappedError() throws Exception {
        when(userService.findById(999L)).thenThrow(new ResourceNotFoundException("Usuario no encontrado con id 999"));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado con id 999"));
    }

    @Test
    void create_asAdmin_returns201AndDelegatesWithAdminId() throws Exception {
        setAdminAuthentication(10L);

        CreateUserRequest request = new CreateUserRequest(
                "Nora Nueva",
                "nora.nueva@docurural.edu.co",
                "supersecreta",
                "supersecreta",
                UserRole.READER);

        CreateUserResponse response = new CreateUserResponse(
                55L,
                "Nora Nueva",
                "nora.nueva@docurural.edu.co",
                "READER",
                "ACTIVE",
                LocalDateTime.of(2026, 1, 2, 9, 0),
                "Usuario creado exitosamente");

        when(userService.create(any(CreateUserRequest.class), eq(10L), any(HttpServletRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.message").value("Usuario creado exitosamente"));

        verify(userService).create(
                argThat(req -> "Nora Nueva".equals(req.fullName())),
                eq(10L),
                any(HttpServletRequest.class));
    }

    @Test
    void create_withInvalidBody_returns400ValidationErrors() throws Exception {
        setAdminAuthentication(10L);

        String body = """
                {
                  "fullName": "",
                  "email": "correo-invalido",
                  "password": "123",
                  "confirmPassword": "999",
                  "role": null
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void update_asAdmin_returns200AndDelegates() throws Exception {
        setAdminAuthentication(10L);

        UpdateUserRequest request = new UpdateUserRequest(
                "Erik Editor Renombrado",
                "erik.editor@docurural.edu.co",
                UserRole.EDITOR,
                null,
                null);

        UpdateUserResponse response = new UpdateUserResponse(
                2L,
                "Erik Editor Renombrado",
                "erik.editor@docurural.edu.co",
                "EDITOR",
                "ACTIVE",
                "Usuario actualizado exitosamente");

        when(userService.update(eq(2L), any(UpdateUserRequest.class), eq(10L), any(HttpServletRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/users/2")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.fullName").value("Erik Editor Renombrado"))
                .andExpect(jsonPath("$.message").value("Usuario actualizado exitosamente"));

        verify(userService).update(
                eq(2L),
                argThat(req -> "Erik Editor Renombrado".equals(req.fullName())
                        && UserRole.EDITOR == req.role()),
                eq(10L),
                any(HttpServletRequest.class));
    }

    @Test
    void update_withInvalidBody_returns400ValidationErrors() throws Exception {
        setAdminAuthentication(10L);

        String body = """
                {
                  "fullName": "",
                  "email": "correo-invalido",
                  "role": null,
                  "password": "123",
                  "confirmPassword": "123"
                }
                """;

        mockMvc.perform(put("/users/2")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void update_whenEmailConflict_returns409MappedError() throws Exception {
        setAdminAuthentication(10L);

        UpdateUserRequest request = new UpdateUserRequest(
                "Erik Editor",
                "existe@docurural.edu.co",
                UserRole.EDITOR,
                null,
                null);

        when(userService.update(eq(2L), any(UpdateUserRequest.class), eq(10L), any(HttpServletRequest.class)))
                .thenThrow(new ConflictException("Ya existe un usuario registrado con este correo electrónico"));

        mockMvc.perform(put("/users/2")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe un usuario registrado con este correo electrónico"));
    }

    @Test
    void update_whenNotFound_returns404MappedError() throws Exception {
        setAdminAuthentication(10L);

        UpdateUserRequest request = new UpdateUserRequest(
                "Erik Editor",
                "erik.editor@docurural.edu.co",
                UserRole.EDITOR,
                null,
                null);

        when(userService.update(eq(999L), any(UpdateUserRequest.class), eq(10L), any(HttpServletRequest.class)))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con id 999"));

        mockMvc.perform(put("/users/999")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado con id 999"));
    }

    @Test
    void changeStatus_asAdmin_returns200() throws Exception {
        setAdminAuthentication(10L);

        UpdateStatusRequest request = new UpdateStatusRequest(UserStatus.INACTIVE);
        UpdateStatusResponse response = new UpdateStatusResponse(
                2L,
                "Erik Editor",
                "INACTIVE",
                "Usuario desactivado exitosamente");

        when(userService.changeStatus(eq(2L), any(UpdateStatusRequest.class), eq(10L), any(HttpServletRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/users/2/status")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.message").value("Usuario desactivado exitosamente"));
    }

    @Test
    void changeStatus_whenSelfDeactivation_returns403MappedError() throws Exception {
        setAdminAuthentication(10L);

        UpdateStatusRequest request = new UpdateStatusRequest(UserStatus.INACTIVE);

        when(userService.changeStatus(eq(10L), any(UpdateStatusRequest.class), eq(10L), any(HttpServletRequest.class)))
                .thenThrow(new BusinessRuleException(
                        HttpStatus.FORBIDDEN,
                        "No puede desactivar su propia cuenta de administrador"));

        mockMvc.perform(patch("/users/10/status")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("No puede desactivar su propia cuenta de administrador"));
    }

    @Test
    void changeStatus_withInvalidEnumValue_returns400MappedError() throws Exception {
        setAdminAuthentication(10L);

        String body = """
                {
                  "status": "VALOR_INVALIDO"
                }
                """;

        mockMvc.perform(patch("/users/2/status")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo de la solicitud es inválido o contiene un valor no permitido"));
    }

    @Test
    void changeStatus_withMissingStatus_returns400ValidationErrors() throws Exception {
        setAdminAuthentication(10L);

        String body = """
                {
                  "status": null
                }
                """;

        mockMvc.perform(patch("/users/2/status")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    private static void setAdminAuthentication(Long id) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                id,
                "ana.admin@docurural.edu.co",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                null);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
