package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta del login: copie el token y péguelo en Authorize")
public record AuthResponse(
        @Schema(description = "JWT válido por 24 horas; incluye los claims rol y solicitanteId",
                example = "eyJhbGciOiJIUzI1NiJ9...") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(example = "admin") String username,
        @Schema(example = "Administrador") String nombre,
        @Schema(example = "ADMIN") String rol,
        @Schema(description = "Solo ALUMNO: id de su solicitante (null para ADMIN y ENCARGADO)", example = "null")
        Long solicitanteId) {
}
