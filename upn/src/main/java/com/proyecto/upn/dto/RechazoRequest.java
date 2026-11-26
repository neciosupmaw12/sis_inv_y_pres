package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Motivo del rechazo de una solicitud (obligatorio)")
public record RechazoRequest(
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(min = 5, max = 255, message = "El motivo debe tener entre 5 y 255 caracteres")
        @Schema(example = "No hay proyectores disponibles para esa fecha") String motivo
) {
}
