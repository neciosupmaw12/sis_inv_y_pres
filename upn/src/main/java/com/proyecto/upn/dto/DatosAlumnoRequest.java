package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del estudiante que se piden al crear un usuario con rol ALUMNO. Con ellos se crea su Solicitante de tipo
 * ESTUDIANTE; si ya existe un solicitante con ese DNI, se vincula ese (sus datos no se modifican).
 */
@Schema(description = "Datos del estudiante (obligatorios cuando rol = ALUMNO)")
public record DatosAlumnoRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
        @Schema(example = "72345678") String dni,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres")
        @Schema(example = "Ana") String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(min = 2, max = 80, message = "Los apellidos deben tener entre 2 y 80 caracteres")
        @Schema(example = "Torres Díaz") String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no debe superar 100 caracteres")
        @Schema(example = "ana.torres@upn.pe") String email,

        @Pattern(regexp = "^$|9\\d{8}", message = "El teléfono debe tener 9 dígitos y empezar con 9")
        @Schema(example = "956789123") String telefono,

        @NotBlank(message = "La carrera es obligatoria")
        @Size(max = 100, message = "La carrera no debe superar 100 caracteres")
        @Schema(example = "Ingeniería Industrial") String area
) {
}
