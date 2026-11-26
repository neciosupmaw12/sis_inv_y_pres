package com.proyecto.upn.controller;

import com.proyecto.upn.config.EjemplosError;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.RegistroRequest;
import com.proyecto.upn.dto.UsuarioResponse;
import com.proyecto.upn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administración de usuarios del sistema (solo rol ADMIN). */
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema: ADMIN, ENCARGADO y ALUMNO (solo ADMIN)")
@RestController
@RequestMapping(path = "/api/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final AuthService authService;

    @Operation(summary = "Listar usuarios")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "USUARIOS_SIN_PERMISO", summary = "Usuario ENCARGADO", value = EjemplosError.USUARIOS_SIN_PERMISO)))
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> index() {
        return ResponseEntity.ok(authService.listar());
    }

    @Operation(summary = "Crear un usuario",
            description = "Para rol ALUMNO envíe además 'alumno' con los datos del estudiante: si ya existe un "
                    + "solicitante ESTUDIANTE con ese DNI se vincula (sin duplicarlo); si no, se crea. Estudiante y "
                    + "usuario se guardan en UNA transacción: ante cualquier 409 no se guarda ninguno de los dos.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "USUARIO_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.USUARIO_DATOS_INVALIDOS),
                            @ExampleObject(name = "USUARIO_ALUMNO_SIN_DATOS", summary = "ALUMNO sin datos del estudiante", value = EjemplosError.USUARIO_ALUMNO_SIN_DATOS),
                            @ExampleObject(name = "USUARIO_ALUMNO_DATOS_INVALIDOS", summary = "Datos del estudiante inválidos", value = EjemplosError.USUARIO_ALUMNO_DATOS_INVALIDOS)}))
    @ApiResponse(responseCode = "409", description = "Usuario duplicado, estudiante ya vinculado, DNI que no es de un "
            + "estudiante o correo duplicado (rollback: no se guarda nada)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "USUARIO_DUPLICADO", summary = "Usuario duplicado", value = EjemplosError.USUARIO_DUPLICADO),
                            @ExampleObject(name = "USUARIO_ALUMNO_YA_VINCULADO", summary = "El estudiante ya tiene usuario", value = EjemplosError.USUARIO_ALUMNO_YA_VINCULADO),
                            @ExampleObject(name = "USUARIO_ALUMNO_DNI_NO_ESTUDIANTE", summary = "DNI de un docente", value = EjemplosError.USUARIO_ALUMNO_DNI_NO_ESTUDIANTE),
                            @ExampleObject(name = "USUARIO_ALUMNO_EMAIL_DUPLICADO", summary = "Correo de otro solicitante", value = EjemplosError.USUARIO_ALUMNO_EMAIL_DUPLICADO)}))
    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** Desactiva (borrado lógico) al usuario. */
    @Operation(summary = "Desactivar un usuario", description = "Borrado lógico; no puede desactivarse a sí mismo.")
    @ApiResponse(responseCode = "204", description = "Usuario desactivado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "USUARIO_NO_ENCONTRADO", summary = "Usuario inexistente", value = EjemplosError.USUARIO_NO_ENCONTRADO)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID del usuario", example = "2") @PathVariable Long id,
                                       @Parameter(hidden = true) Authentication auth) {
        authService.eliminar(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
