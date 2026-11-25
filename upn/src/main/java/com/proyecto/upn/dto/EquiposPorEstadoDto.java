package com.proyecto.upn.dto;

import com.proyecto.upn.entity.EstadoEquipo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Proyección de la consulta JPQL de agregación:
 * {@code SELECT new com.proyecto.upn.dto.EquiposPorEstadoDto(e.estado, COUNT(e), SUM(e.cantidadTotal), SUM(e.cantidadDisponible))
 * FROM Equipo e GROUP BY e.estado}
 */
@Schema(description = "Equipos de un estado")
public record EquiposPorEstadoDto(
        @Schema(example = "OPERATIVO") EstadoEquipo estado,
        @Schema(description = "Tipos de equipo en ese estado", example = "6") Long equipos,
        @Schema(example = "41") Long unidadesTotales,
        @Schema(example = "34") Long unidadesDisponibles
) {
}
