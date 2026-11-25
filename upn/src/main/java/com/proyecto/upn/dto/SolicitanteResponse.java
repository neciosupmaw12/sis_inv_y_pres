package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.entity.TipoSolicitante;
import io.swagger.v3.oas.annotations.media.Schema;

/** Datos de un solicitante que se devuelven en los listados paginados. */
@Schema(description = "Solicitante de préstamos")
public record SolicitanteResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "70123456") String dni,
        @Schema(example = "María") String nombres,
        @Schema(example = "Quispe Torres") String apellidos,
        @Schema(example = "ESTUDIANTE") TipoSolicitante tipo,
        @Schema(example = "Ingeniería de Sistemas") String area,
        @Schema(example = "maria.quispe@upn.pe") String email,
        @Schema(example = "987654321") String telefono
) {
    public static SolicitanteResponse de(Solicitante s) {
        return new SolicitanteResponse(s.getId(), s.getDni(), s.getNombres(), s.getApellidos(), s.getTipo(),
                s.getArea(), s.getEmail(), s.getTelefono());
    }
}
