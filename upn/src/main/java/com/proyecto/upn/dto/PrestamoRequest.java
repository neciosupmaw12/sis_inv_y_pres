package com.proyecto.upn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PrestamoRequest(
        @NotNull(message = "El solicitante es obligatorio")
        Long solicitanteId,

        @NotNull(message = "La fecha de devolución es obligatoria")
        @FutureOrPresent(message = "La fecha de devolución no puede ser anterior a hoy")
        LocalDate fechaDevolucionPrevista,

        @Size(max = 255, message = "Las observaciones no deben superar 255 caracteres")
        String observaciones,

        @NotEmpty(message = "El préstamo debe incluir al menos un equipo")
        List<@Valid @NotNull DetallePrestamoRequest> detalles
) {
}
