package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario del sistema")
public record UsuarioResponse(
        @Schema(example = "8") Long id,
        @Schema(example = "alumno") String username,
        @Schema(example = "Alumno Prueba UPN") String nombre,
        @Schema(example = "ALUMNO") String rol,
        @Schema(example = "true") Boolean activo,
        @Schema(description = "Solo ALUMNO: id del solicitante vinculado", example = "5") Long solicitanteId,
        @Schema(description = "Solo ALUMNO: DNI del estudiante vinculado", example = "71234567") String dni,
        @Schema(description = "Solo ALUMNO: carrera del estudiante", example = "Ingeniería de Sistemas") String carrera) {

    public static UsuarioResponse de(Usuario u) {
        Solicitante s = u.getSolicitante();
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombre(), u.getRol().name(), u.getActivo(),
                s == null ? null : s.getId(), s == null ? null : s.getDni(), s == null ? null : s.getArea());
    }
}
