package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un usuario. Para ADMIN y ENCARGADO se usa {@code nombre}; para ALUMNO se envía {@code alumno} y el nombre
 * del usuario se toma de los nombres y apellidos de su Solicitante (el campo {@code nombre} se ignora).
 */
@Schema(description = "Datos para crear un usuario del sistema")
public record RegistroRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._]{4,50}$", message = "El usuario debe tener de 4 a 50 caracteres alfanuméricos")
        @Schema(example = "jperez") String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 60, message = "La contraseña debe tener entre 6 y 60 caracteres")
        @Schema(example = "clave123") String password,

        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        @Schema(description = "Obligatorio para ADMIN y ENCARGADO; para ALUMNO se ignora", example = "Juan Pérez")
        String nombre,

        @NotNull(message = "El rol es obligatorio (ADMIN, ENCARGADO o ALUMNO)")
        @Schema(example = "ENCARGADO") Rol rol,

        @Valid
        @Schema(description = "Solo para rol ALUMNO: datos del estudiante que se vincula al usuario")
        DatosAlumnoRequest alumno
) {
}
