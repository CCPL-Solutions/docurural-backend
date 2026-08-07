package co.edu.docurural.health.controller;

import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con un bean {@code BuildProperties} presente (generado por el goal build-info de Maven
 * en el pipeline de CI/CD), el controlador debe exponer la versión y el commit reales.
 */
@WebMvcTest(
        controllers = VersionController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VersionControllerWithBuildInfoWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BuildProperties buildProperties;

    @Test
    void getVersion_returnsRealBuildMetadata_whenBuildPropertiesBeanPresent() throws Exception {
        when(buildProperties.getVersion()).thenReturn("1.0.0-rc.3");
        when(buildProperties.get("commit")).thenReturn("abc1234");
        when(buildProperties.get("branch")).thenReturn("release/1.0.0");
        when(buildProperties.getTime()).thenReturn(Instant.parse("2026-08-06T10:00:00Z"));

        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0-rc.3"))
                .andExpect(jsonPath("$.commit").value("abc1234"))
                .andExpect(jsonPath("$.branch").value("release/1.0.0"))
                .andExpect(jsonPath("$.buildTime").exists());
    }
}
