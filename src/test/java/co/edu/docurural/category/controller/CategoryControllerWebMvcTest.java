package co.edu.docurural.category.controller;

import co.edu.docurural.category.dto.CategoryDetailResponseDto;
import co.edu.docurural.category.dto.CategoryListResponseDto;
import co.edu.docurural.category.dto.CreateCategoryRequestDto;
import co.edu.docurural.category.dto.CreateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryResponseDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusRequestDto;
import co.edu.docurural.category.dto.UpdateCategoryStatusResponseDto;
import co.edu.docurural.category.enums.CategoryStatus;
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

        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest(
                "Proyectos Biotecnología",
                "Proyectos e informes del laboratorio");

        CreateCategoryResponseDto response = new CreateCategoryResponseDto(
                9L,
                "Proyectos Biotecnología",
                "Proyectos e informes del laboratorio",
                "ACTIVE",
                LocalDateTime.of(2026, 4, 17, 10, 15),
                "INTERNAL",
                "Categoría creada exitosamente");

        when(categoryService.create(any(CreateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest("Circulares", null);

        CreateCategoryResponseDto response = new CreateCategoryResponseDto(
                10L, "Circulares", null, "ACTIVE",
                LocalDateTime.of(2026, 4, 17, 10, 15),
                "INTERNAL",
                "Categoría creada exitosamente");

        when(categoryService.create(any(CreateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        CreateCategoryRequestDto request = TestFixtures.createCategoryRequest("Actas", null);

        when(categoryService.create(any(CreateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest(
                "Proyectos e Informes Biotecnología",
                "Proyectos e informes detallados");

        UpdateCategoryResponseDto response = new UpdateCategoryResponseDto(
                9L,
                "Proyectos e Informes Biotecnología",
                "Proyectos e informes detallados",
                "ACTIVE",
                "INTERNAL",
                "Categoría actualizada exitosamente");

        when(categoryService.update(eq(9L), any(UpdateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Circulares", null);

        UpdateCategoryResponseDto response = new UpdateCategoryResponseDto(
                10L, "Circulares", null, "ACTIVE", "INTERNAL", "Categoría actualizada exitosamente");

        when(categoryService.update(eq(10L), any(UpdateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Nombre Válido", null);

        when(categoryService.update(eq(99L), any(UpdateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Nombre Válido", null);

        when(categoryService.update(eq(5L), any(UpdateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
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

        UpdateCategoryRequestDto request = TestFixtures.updateCategoryRequest("Actas", null);

        when(categoryService.update(eq(9L), any(UpdateCategoryRequestDto.class), eq(ADMIN_AUDIT)))
                .thenThrow(new ConflictException("Ya existe una categoría con este nombre"));

        mockMvc.perform(put("/categories/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe una categoría con este nombre"));
    }

    // ------------------------------------------------------------------
    // PATCH /categories/{id}/status
    // ------------------------------------------------------------------

    @Test
    void changeStatus_returns200AndPayload_whenDeactivating() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryStatusRequestDto request = TestFixtures.updateCategoryStatusRequest(CategoryStatus.INACTIVE);

        UpdateCategoryStatusResponseDto response = new UpdateCategoryStatusResponseDto(
                9L, "Proyectos e Informes Biotecnología", "INACTIVE", "Categoría desactivada exitosamente");

        when(categoryService.changeStatus(eq(9L), any(UpdateCategoryStatusRequestDto.class), eq(ADMIN_AUDIT)))
                .thenReturn(response);

        mockMvc.perform(patch("/categories/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Proyectos e Informes Biotecnología"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.message").value("Categoría desactivada exitosamente"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        verify(categoryService).changeStatus(
                eq(9L),
                argThat(req -> CategoryStatus.INACTIVE.equals(req.status())),
                eq(ADMIN_AUDIT));
    }

    @Test
    void changeStatus_returns200AndPayload_whenReactivating() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        UpdateCategoryStatusRequestDto request = TestFixtures.updateCategoryStatusRequest(CategoryStatus.ACTIVE);

        UpdateCategoryStatusResponseDto response = new UpdateCategoryStatusResponseDto(
                9L, "Proyectos e Informes Biotecnología", "ACTIVE", "Categoría activada exitosamente");

        when(categoryService.changeStatus(eq(9L), any(UpdateCategoryStatusRequestDto.class), eq(ADMIN_AUDIT)))
                .thenReturn(response);

        mockMvc.perform(patch("/categories/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Categoría activada exitosamente"));
    }

    @Test
    void changeStatus_withNullStatus_returns400WithFieldError() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        mockMvc.perform(patch("/categories/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void changeStatus_withInvalidStatus_returns400() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        mockMvc.perform(patch("/categories/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void changeStatus_whenCategoryNotFound_returns404() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        when(categoryService.changeStatus(eq(99L), any(UpdateCategoryStatusRequestDto.class), eq(ADMIN_AUDIT)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con id 99"));

        mockMvc.perform(patch("/categories/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada con id 99"));
    }

    @Test
    void changeStatus_whenAlreadyInStatus_returns400() throws Exception {
        when(auditContextResolver.resolve(any())).thenReturn(ADMIN_AUDIT);

        when(categoryService.changeStatus(eq(9L), any(UpdateCategoryStatusRequestDto.class), eq(ADMIN_AUDIT)))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.INVALID_ARGUMENT, "La categoría ya se encuentra en el estado solicitado"));

        mockMvc.perform(patch("/categories/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("La categoría ya se encuentra en el estado solicitado"));
    }

    // ------------------------------------------------------------------
    // GET /categories
    // ------------------------------------------------------------------

    @Test
    void list_returns200WithListResponse() throws Exception {
        CategoryDetailResponseDto item = new CategoryDetailResponseDto(
                1L, "Actas", "Actas de reuniones, consejos directivos",
                "ACTIVE", 23L, LocalDateTime.of(2026, 4, 1, 8, 0), "Sistema", "INTERNAL");

        CategoryListResponseDto response = new CategoryListResponseDto(1, 1, 0, List.of(item));

        when(categoryService.list(any(), any())).thenReturn(response);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCategories").value(1))
                .andExpect(jsonPath("$.activeCategories").value(1))
                .andExpect(jsonPath("$.inactiveCategories").value(0))
                .andExpect(jsonPath("$.categories[0].id").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("Actas"))
                .andExpect(jsonPath("$.categories[0].documentCount").value(23))
                .andExpect(jsonPath("$.categories[0].createdBy").value("Sistema"));
    }

    @Test
    void list_withSortParams_passesParamsToService() throws Exception {
        when(categoryService.list("createdAt", "desc"))
                .thenReturn(new CategoryListResponseDto(0, 0, 0, List.of()));

        mockMvc.perform(get("/categories")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());

        verify(categoryService).list("createdAt", "desc");
    }

    @Test
    void list_withInvalidSortBy_returns400() throws Exception {
        when(categoryService.list("foo", null))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.INVALID_ARGUMENT, "Campo de ordenamiento no soportado: foo"));

        mockMvc.perform(get("/categories").param("sortBy", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Campo de ordenamiento no soportado: foo"));
    }

    @Test
    void list_withInvalidSortDir_returns400() throws Exception {
        when(categoryService.list(null, "ascending"))
                .thenThrow(new BusinessRuleException(
                        BusinessErrorCode.INVALID_ARGUMENT, "Dirección de ordenamiento no soportada: ascending"));

        mockMvc.perform(get("/categories").param("sortDir", "ascending"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Dirección de ordenamiento no soportada: ascending"));
    }

    // ------------------------------------------------------------------
    // GET /categories/{id}
    // ------------------------------------------------------------------

    @Test
    void getById_returns200WithDetailResponse() throws Exception {
        CategoryDetailResponseDto response = new CategoryDetailResponseDto(
                1L, "Actas", "Actas de reuniones, consejos directivos",
                "ACTIVE", 23L, LocalDateTime.of(2026, 4, 1, 8, 0), "Sistema", "INTERNAL");

        when(categoryService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Actas"))
                .andExpect(jsonPath("$.description").value("Actas de reuniones, consejos directivos"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.documentCount").value(23))
                .andExpect(jsonPath("$.createdBy").value("Sistema"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(categoryService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con id 99"));

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada con id 99"));
    }

    @Test
    void getById_returnsCreatedBySistema_whenCreatorMissing() throws Exception {
        CategoryDetailResponseDto response = new CategoryDetailResponseDto(
                2L, "Resoluciones", null, "ACTIVE", 0L,
                LocalDateTime.of(2026, 4, 1, 8, 0), "Sistema", "INTERNAL");

        when(categoryService.findById(2L)).thenReturn(response);

        mockMvc.perform(get("/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value("Sistema"));
    }
}
