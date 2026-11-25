package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Indicadores del panel de inicio, calculados con consultas JPQL de agregación. */
@Schema(description = "Indicadores del panel de inicio")
public record PanelResumenResponse(
        @Schema(description = "Equipos agrupados por estado") List<EquiposPorEstadoDto> equiposPorEstado,
        @Schema(example = "44") long unidadesTotales,
        @Schema(description = "Unidades en almacén de equipos OPERATIVOS", example = "37") long unidadesDisponibles,
        @Schema(description = "Unidades que están prestadas", example = "7") long unidadesPrestadas,
        @Schema(description = "Conteo de préstamos por estado") ConteoPrestamosDto prestamos,
        @Schema(description = "Los 5 equipos con más unidades prestadas en el historial") List<EquipoMasPrestadoDto> topEquipos
) {
}
