package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Solicitud de préstamo de un ALUMNO. No lleva solicitanteId: el backend lo toma del JWT.
 * Límites: máx. 3 unidades por equipo y 5 en total por solicitud (se revalidan en el servicio).
 */
@Schema(description = "Solicitud de préstamo de un alumno (queda PENDIENTE hasta que el ADMIN la apruebe)")
public record SolicitudRequest(
        @NotNull(message = "La fecha de devolución es obligatoria")
        @FutureOrPresent(message = "La fecha de devolución no puede ser anterior a hoy")
        @Schema(description = "Hoy o una fecha posterior", example = "2026-12-03") LocalDate fechaDevolucionPrevista,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 255, message = "El motivo debe tener entre 5 y 255 caracteres")
        @Schema(example = "Exposición del curso de Redes, aula 305") String motivo,

        @NotEmpty(message = "La solicitud debe incluir al menos un equipo")
        @Size(max = 5, message = "La solicitud puede incluir como máximo 5 equipos distintos")
        @Schema(description = "Equipos y cantidades (al menos uno)")
        List<@Valid @NotNull DetalleSolicitudRequest> detalles
) {
    @Schema(description = "Equipo y cantidad dentro de una solicitud")
    public record DetalleSolicitudRequest(
            @NotNull(message = "El equipo es obligatorio")
            @Schema(example = "3") Long equipoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad mínima es 1")
            @Max(value = 3, message = "Puede pedir como máximo 3 unidades de cada equipo")
            @Schema(example = "1", minimum = "1", maximum = "3") Integer cantidad
    ) {
    }
}
