package com.proyecto.upn.dto;

import com.proyecto.upn.entity.TipoSolicitante;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para registrar o actualizar un solicitante")
public record SolicitanteRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
        @Schema(example = "72345678") String dni,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres")
        @Schema(example = "Ana") String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(min = 2, max = 80, message = "Los apellidos deben tener entre 2 y 80 caracteres")
        @Schema(example = "Torres Díaz") String apellidos,

        @NotNull(message = "El tipo es obligatorio (ESTUDIANTE, DOCENTE o ADMINISTRATIVO)")
        @Schema(example = "ESTUDIANTE") TipoSolicitante tipo,

        @Size(max = 100, message = "La carrera o área no debe superar 100 caracteres")
        @Schema(example = "Ingeniería Industrial") String area,

        @Email(message = "El email no tiene un formato válido")
        @Size(max = 100, message = "El email no debe superar 100 caracteres")
        @Schema(example = "ana.torres@upn.pe") String email,

        @Pattern(regexp = "^$|9\\d{8}", message = "El teléfono debe tener 9 dígitos y empezar con 9")
        @Schema(example = "956789123") String telefono
) {
}
