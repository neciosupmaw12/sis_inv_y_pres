package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Proyección de la consulta JPQL "top de equipos más prestados":
 * {@code SELECT new com.proyecto.upn.dto.EquipoMasPrestadoDto(eq.id, eq.codigo, eq.nombre, SUM(d.cantidad), COUNT(DISTINCT p.id))
 * FROM DetallePrestamo d JOIN d.equipo eq JOIN d.prestamo p GROUP BY ... ORDER BY SUM(d.cantidad) DESC}
 */
@Schema(description = "Equipo con su total de unidades prestadas")
public record EquipoMasPrestadoDto(
        @Schema(example = "8") Long id,
        @Schema(example = "LAP-009") String codigo,
        @Schema(example = "Laptop ASUS") String nombre,
        @Schema(description = "Unidades prestadas en todo el historial", example = "13") Long unidadesPrestadas,
        @Schema(description = "Préstamos en los que aparece", example = "4") Long prestamos
) {
}
