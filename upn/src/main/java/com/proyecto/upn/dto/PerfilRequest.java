package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lo único que el alumno puede cambiar de su perfil: correo y teléfono (DNI, nombres y carrera los cambia el personal). */
@Schema(description = "Datos de contacto que el alumno puede editar")
public record PerfilRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no debe superar 100 caracteres")
        @Schema(example = "alumno.prueba@upn.pe") String email,

        @Pattern(regexp = "^$|9\\d{8}", message = "El teléfono debe tener 9 dígitos y empezar con 9")
        @Schema(example = "951234567") String telefono
) {
}
