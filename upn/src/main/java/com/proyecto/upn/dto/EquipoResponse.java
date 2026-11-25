package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Equipo;
import com.proyecto.upn.entity.EstadoEquipo;
import io.swagger.v3.oas.annotations.media.Schema;

/** Datos de un equipo que se devuelven en los listados paginados (no expone la entidad JPA). */
@Schema(description = "Equipo del inventario")
public record EquipoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "LAP-001") String codigo,
        @Schema(example = "Laptop Lenovo ThinkPad E14") String nombre,
        @Schema(example = "Laptops") String categoria,
        @Schema(example = "Lenovo") String marca,
        @Schema(example = "ThinkPad E14 Gen 5") String modelo,
        String descripcion,
        @Schema(description = "Unidades que posee la institución", example = "10") Integer cantidadTotal,
        @Schema(description = "Unidades en almacén (total menos prestadas)", example = "8") Integer cantidadDisponible,
        @Schema(example = "OPERATIVO") EstadoEquipo estado
) {
    public static EquipoResponse de(Equipo e) {
        return new EquipoResponse(e.getId(), e.getCodigo(), e.getNombre(), e.getCategoria(), e.getMarca(),
                e.getModelo(), e.getDescripcion(), e.getCantidadTotal(), e.getCantidadDisponible(), e.getEstado());
    }
}
