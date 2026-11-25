package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de acceso")
public record LoginRequest(
        @Schema(example = "admin") @NotBlank(message = "El usuario es obligatorio") String username,
        @Schema(example = "admin123") @NotBlank(message = "La contraseña es obligatoria") String password
) {
}
