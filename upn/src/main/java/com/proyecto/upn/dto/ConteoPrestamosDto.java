package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Proyección de la agregación JPQL con {@code SUM(CASE WHEN ... THEN 1 ELSE 0 END)} sobre préstamos.
 * VENCIDO no se guarda en la base: es un préstamo ACTIVO cuya fecha prevista ya pasó.
 */
@Schema(description = "Cantidad de préstamos por estado")
public record ConteoPrestamosDto(
        @Schema(example = "6") Long total,
        @Schema(description = "Activos dentro de plazo", example = "1") Long activos,
        @Schema(description = "Activos con la fecha prevista vencida", example = "1") Long vencidos,
        @Schema(example = "4") Long devueltos
) {
}
