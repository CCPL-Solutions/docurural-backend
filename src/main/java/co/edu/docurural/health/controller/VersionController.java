package co.edu.docurural.health.controller;

import co.edu.docurural.health.dto.VersionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la versión y el commit del artefacto en ejecución, para que QA pueda
 * confirmar exactamente qué build está certificando durante el ciclo de release.
 */
@RestController
@RequestMapping("/version")
@RequiredArgsConstructor
@Tag(name = "Version", description = "Metadata de build del artefacto desplegado")
public class VersionController {

    private static final String UNKNOWN = "unknown";

    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @GetMapping
    @Operation(summary = "Obtener versión y metadata de build",
            description = "Retorna la versión semántica, el commit, la rama y la fecha de build "
                    + "generados por el pipeline de CI/CD. Si el artefacto no fue construido con "
                    + "Maven (ej. ejecución local sin el goal build-info), retorna valores 'unknown'.")
    public ResponseEntity<VersionResponseDto> getVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties == null) {
            return ResponseEntity.ok(new VersionResponseDto(UNKNOWN, UNKNOWN, UNKNOWN, null));
        }
        return ResponseEntity.ok(new VersionResponseDto(
                buildProperties.getVersion(),
                buildProperties.get("commit"),
                buildProperties.get("branch"),
                buildProperties.getTime()));
    }
}
