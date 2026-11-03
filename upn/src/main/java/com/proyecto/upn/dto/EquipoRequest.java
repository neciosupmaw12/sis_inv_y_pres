package com.proyecto.upn.dto;

import com.proyecto.upn.entity.EstadoEquipo;
import jakarta.validation.constraints.*;

public record EquipoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no debe superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 50, message = "La categoría no debe superar 50 caracteres")
        String categoria,

        @Size(max = 50, message = "La marca no debe superar 50 caracteres")
        String marca,

        @Size(max = 50, message = "El modelo no debe superar 50 caracteres")
        String modelo,

        @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
        String descripcion,

        @NotNull(message = "La cantidad total es obligatoria")
        @Min(value = 1, message = "La cantidad total mínima es 1")
        @Max(value = 1000, message = "La cantidad total máxima es 1000")
        Integer cantidadTotal,

        @NotNull(message = "El estado es obligatorio (OPERATIVO, EN_MANTENIMIENTO o DE_BAJA)")
        EstadoEquipo estado
) {
}
