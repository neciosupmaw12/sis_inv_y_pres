package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Datos para registrar o editar un préstamo con uno o varios equipos")
public record PrestamoRequest(
        @NotNull(message = "El solicitante es obligatorio")
        @Schema(example = "1") Long solicitanteId,

        @NotNull(message = "La fecha de devolución es obligatoria")
        @FutureOrPresent(message = "La fecha de devolución no puede ser anterior a hoy")
        @Schema(description = "Hoy o una fecha posterior", example = "2026-12-01") LocalDate fechaDevolucionPrevista,

        @Size(max = 255, message = "Las observaciones no deben superar 255 caracteres")
        @Schema(example = "Clase de Redes, laboratorio 305") String observaciones,

        @NotEmpty(message = "El préstamo debe incluir al menos un equipo")
        @Schema(description = "Equipos y cantidades (al menos uno)") List<@Valid @NotNull DetallePrestamoRequest> detalles
) {
}
