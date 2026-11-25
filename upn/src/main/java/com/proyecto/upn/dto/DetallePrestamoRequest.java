package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Equipo y cantidad dentro de un préstamo")
public record DetallePrestamoRequest(
        @NotNull(message = "El equipo es obligatorio")
        @Schema(example = "1") Long equipoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = 100, message = "La cantidad máxima es 100")
        @Schema(example = "2", minimum = "1", maximum = "100") Integer cantidad
) {
}
