package co.edu.docurural.category.controller;

import co.edu.docurural.category.dto.CreateCategoryRequest;
import co.edu.docurural.category.dto.CreateCategoryResponse;
import co.edu.docurural.category.dto.UpdateCategoryRequest;
import co.edu.docurural.category.dto.UpdateCategoryResponse;
import co.edu.docurural.category.service.CategoryService;
import co.edu.docurural.shared.audit.AuditContext;
import co.edu.docurural.shared.audit.AuditContextResolver;
import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import co.edu.docurural.shared.exception.ConflictException;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.exception.ResourceNotFoundException;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import co.edu.docurural.support.TestFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CategoryController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerWebMvcTest {

    private static final AuditContext ADMIN_AUDIT = new AuditContext(10L, "127.0.0.1");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CategoryService categoryService;
    @MockitoBean
    AuditContextResolver auditContextResolver;

    @Test
    void create_returns201AndPayload() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        CreateCategoryRequest request = TestFixtures.createCategoryRequest(
                "Proyectos Biotecnología",
                "Proyectos e informes del laboratorio");

        CreateCategoryResponse response = new CreateCategoryResponse(
                9L,
                "Proyectos Biotecnología",
                "Proyectos e informes del laboratorio",
                "ACTIVE",
                LocalDateTime.of(2026, 4, 17, 10, 15),
                "Categoría creada exitosamente");

        when(categoryService.create(any(CreateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenReturn(response);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Proyectos Biotecnología"))
                .andExpect(jsonPath("$.description").value("Proyectos e informes del laboratorio"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Categoría creada exitosamente"));

        verify(categoryService).create(
                argThat(req -> "Proyectos Biotecnología".equals(req.name())),
                eq(ADMIN_AUDIT));
    }

    @Test
    void create_withNullDescription_returns201() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        CreateCategoryRequest request = TestFixtures.createCategoryRequest("Circulares", null);

        CreateCategoryResponse response = new CreateCategoryResponse(
                10L, "Circulares", null, "ACTIVE",
                LocalDateTime.of(2026, 4, 17, 10, 15),
                "Categoría creada exitosamente");

        when(categoryService.create(any(CreateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenReturn(response);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void create_withBlankName_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        String body = """
                {
                  "name": "",
                  "description": "Descripción válida"
                }
                """;

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_withNameTooShort_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        String body = """
                {
                  "name": "AB"
                }
                """;

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_withNameTooLong_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        String longName = "A".repeat(101);
        String body = String.format("{\"name\": \"%s\"}", longName);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_whenNameAlreadyExists_returns409() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        CreateCategoryRequest request = TestFixtures.createCategoryRequest("Actas", null);

        when(categoryService.create(any(CreateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenThrow(new ConflictException("Ya existe una categoría con este nombre"));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe una categoría con este nombre"));
    }

    // ------------------------------------------------------------------
    // PUT /categories/{id}
    // ------------------------------------------------------------------

    @Test
    void update_returns200AndPayload() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest(
                "Proyectos e Informes Biotecnología",
                "Proyectos e informes detallados");

        UpdateCategoryResponse response = new UpdateCategoryResponse(
                9L,
                "Proyectos e Informes Biotecnología",
                "Proyectos e informes detallados",
                "ACTIVE",
                "Categoría actualizada exitosamente");

        when(categoryService.update(eq(9L), any(UpdateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenReturn(response);

        mockMvc.perform(put("/categories/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Proyectos e Informes Biotecnología"))
                .andExpect(jsonPath("$.description").value("Proyectos e informes detallados"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Categoría actualizada exitosamente"));

        verify(categoryService).update(
                eq(9L),
                argThat(req -> "Proyectos e Informes Biotecnología".equals(req.name())),
                eq(ADMIN_AUDIT));
    }

    @Test
    void update_withNullDescription_returns200() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Circulares", null);

        UpdateCategoryResponse response = new UpdateCategoryResponse(
                10L, "Circulares", null, "ACTIVE", "Categoría actualizada exitosamente");

        when(categoryService.update(eq(10L), any(UpdateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenReturn(response);

        mockMvc.perform(put("/categories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void update_withBlankName_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        String body = """
                {
                  "name": "",
                  "description": "Descripción válida"
                }
                """;

        mockMvc.perform(put("/categories/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void update_withNameTooShort_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        String body = """
                {
                  "name": "AB"
                }
                """;

        mockMvc.perform(put("/categories/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void update_withNameTooLong_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        String longName = "A".repeat(101);
        String body = String.format("{\"name\": \"%s\"}", longName);

        mockMvc.perform(put("/categories/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void update_whenCategoryNotFound_returns404() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Nombre Válido", null);

        when(categoryService.update(eq(99L), any(UpdateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con id 99"));

        mockMvc.perform(put("/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada con id 99"));
    }

    @Test
    void update_whenCategoryInactive_returns403() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Nombre Válido", null);

        when(categoryService.update(eq(5L), any(UpdateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.FORBIDDEN, "No se puede editar una categoría inactiva"));

        mockMvc.perform(put("/categories/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("No se puede editar una categoría inactiva"));
    }

    @Test
    void update_whenNameAlreadyExists_returns409() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryRequest request = TestFixtures.updateCategoryRequest("Actas", null);

        when(categoryService.update(eq(9L), any(UpdateCategoryRequest.class), eq(ADMIN_AUDIT)))
                .thenThrow(new ConflictException("Ya existe una categoría con este nombre"));

        mockMvc.perform(put("/categories/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe una categoría con este nombre"));
    }
}
