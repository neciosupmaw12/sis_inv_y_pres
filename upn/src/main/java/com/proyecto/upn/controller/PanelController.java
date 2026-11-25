package com.proyecto.upn.controller;

import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.PanelResumenResponse;
import com.proyecto.upn.service.PanelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Panel", description = "Indicadores del panel de inicio")
@RestController
@RequestMapping("/api/panel")
@RequiredArgsConstructor
public class PanelController {

    private final PanelService panelService;

    @Operation(summary = "Resumen del panel",
            description = "Equipos y unidades por estado, unidades prestadas, préstamos por estado y los 5 equipos "
                    + "más prestados. Se calcula con consultas JPQL de agregación (GROUP BY, SUM, CASE WHEN) y "
                    + "proyecciones SELECT new.")
    @ApiResponse(responseCode = "200", description = "Indicadores calculados")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/resumen")
    public ResponseEntity<PanelResumenResponse> resumen() {
        return ResponseEntity.ok(panelService.resumen());
    }
}
