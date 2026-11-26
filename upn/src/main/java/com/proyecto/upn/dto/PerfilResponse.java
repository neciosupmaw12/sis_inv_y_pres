package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Solicitante;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil del alumno autenticado (su usuario y su solicitante vinculado)")
public record PerfilResponse(
        @Schema(example = "alumno") String username,
        @Schema(example = "6") Long solicitanteId,
        @Schema(example = "71234567") String dni,
        @Schema(example = "Alumno") String nombres,
        @Schema(example = "Prueba UPN") String apellidos,
        @Schema(example = "ESTUDIANTE") String tipo,
        @Schema(example = "Ingeniería de Sistemas") String carrera,
        @Schema(example = "alumno.prueba@upn.pe") String email,
        @Schema(example = "951234567") String telefono) {

    public static PerfilResponse de(String username, Solicitante s) {
        return new PerfilResponse(username, s.getId(), s.getDni(), s.getNombres(), s.getApellidos(),
                s.getTipo().name(), s.getArea(), s.getEmail(), s.getTelefono());
    }
}
