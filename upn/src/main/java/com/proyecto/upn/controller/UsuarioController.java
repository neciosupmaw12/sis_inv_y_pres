package com.proyecto.upn.controller;

import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.RegistroRequest;
import com.proyecto.upn.dto.UsuarioResponse;
import com.proyecto.upn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administración de usuarios del sistema (solo rol ADMIN). */
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema (solo ADMIN)")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final AuthService authService;

    @Operation(summary = "Listar usuarios")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> index() {
        return ResponseEntity.ok(authService.listar());
    }

    @Operation(summary = "Crear un usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El nombre de usuario ya existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** Desactiva (borrado lógico) al usuario. */
    @Operation(summary = "Desactivar un usuario", description = "Borrado lógico; no puede desactivarse a sí mismo.")
    @ApiResponse(responseCode = "204", description = "Usuario desactivado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID del usuario", example = "2") @PathVariable Long id,
                                       @Parameter(hidden = true) Authentication auth) {
        authService.eliminar(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
