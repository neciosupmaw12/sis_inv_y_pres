package com.proyecto.upn.controller;

import com.proyecto.upn.config.EjemplosError;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.PerfilRequest;
import com.proyecto.upn.dto.PerfilResponse;
import com.proyecto.upn.security.SesionJwt;
import com.proyecto.upn.service.SolicitanteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Perfil del alumno autenticado: su solicitante sale del JWT, así que solo puede ver y editar el suyo. */
@Tag(name = "Perfil", description = "Datos del alumno autenticado (solo ALUMNO): ver su perfil y editar correo y teléfono")
@RestController
@RequestMapping(path = "/api/perfil", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ALUMNO')")
public class PerfilController {

    private final SolicitanteService solicitanteService;

    @Operation(summary = "Ver mi perfil (ALUMNO)")
    @ApiResponse(responseCode = "200", description = "Perfil del alumno")
    @ApiResponse(responseCode = "403", description = "El rol no es ALUMNO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PERFIL_SIN_PERMISO", summary = "Usuario ADMIN o ENCARGADO", value = EjemplosError.PERFIL_SIN_PERMISO)))
    @GetMapping
    public ResponseEntity<PerfilResponse> ver(@Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(solicitanteService.perfil(SesionJwt.solicitanteId(auth), auth.getName()));
    }

    @Operation(summary = "Editar mi correo y teléfono (ALUMNO)",
            description = "DNI, nombres, apellidos y carrera solo los cambia el personal en Solicitantes.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Correo o teléfono con formato inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PERFIL_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.PERFIL_DATOS_INVALIDOS)))
    @ApiResponse(responseCode = "409", description = "El correo ya es de otro solicitante",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PERFIL_EMAIL_DUPLICADO", summary = "Correo duplicado", value = EjemplosError.PERFIL_EMAIL_DUPLICADO)))
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PerfilResponse> actualizar(@Valid @RequestBody PerfilRequest request,
                                                     @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(solicitanteService.actualizarContacto(SesionJwt.solicitanteId(auth), auth.getName(),
                request));
    }
}
