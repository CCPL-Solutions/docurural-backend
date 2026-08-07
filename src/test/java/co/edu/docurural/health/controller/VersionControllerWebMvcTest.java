package co.edu.docurural.health.controller;

import co.edu.docurural.shared.config.SecurityConfig;
import co.edu.docurural.shared.exception.GlobalExceptionHandler;
import co.edu.docurural.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin un bean {@code BuildProperties} registrado (equivalente a una ejecución local
 * sin el goal build-info de Maven), el controlador debe responder con valores 'unknown'
 * en vez de fallar al arrancar.
 */
@WebMvcTest(
        controllers = VersionController.class,
        properties = "server.servlet.context-path=",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VersionControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void getVersion_returnsUnknownValues_whenBuildPropertiesBeanMissing() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("unknown"))
                .andExpect(jsonPath("$.commit").value("unknown"))
                .andExpect(jsonPath("$.branch").value("unknown"))
                .andExpect(jsonPath("$.buildTime").doesNotExist());
    }
}
