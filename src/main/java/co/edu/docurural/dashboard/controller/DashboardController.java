package co.edu.docurural.dashboard.controller;

import co.edu.docurural.dashboard.dto.DashboardStatsResponse;
import co.edu.docurural.dashboard.service.DashboardService;
import co.edu.docurural.shared.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint agregado para el panel de control (DSH-01 / HU-24, HU-25, HU-26, HU-27).
 * Retorna en una sola llamada todos los datos necesarios para renderizar el dashboard.
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Panel de control y estadísticas del repositorio (HU-24..HU-27)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','READER')")
    @Operation(
            summary = "Obtener datos del dashboard",
            description = "Retorna en una sola llamada los totales del repositorio, la distribución "
                    + "de documentos activos por categoría y los últimos 10 documentos cargados. "
                    + "Diseñado para minimizar roundtrips en conexiones lentas (DSH-01 / HU-24, HU-25, HU-26).")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Datos del dashboard retornados exitosamente. "
                            + "Los arrays pueden estar vacíos si el repositorio no tiene documentos.",
                    content = @Content(schema = @Schema(implementation = DashboardStatsResponse.class))),
            @ApiResponse(responseCode = "401",
                    description = "Token JWT ausente, inválido o expirado.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500",
                    description = "Error inesperado del servidor.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
