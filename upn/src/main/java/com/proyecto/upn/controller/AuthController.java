package com.proyecto.upn.controller;

import com.proyecto.upn.dto.AuthResponse;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.LoginRequest;
import com.proyecto.upn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Inicio de sesión: devuelve el token JWT")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/login → 200 con el JWT | 401 si las credenciales son incorrectas */
    @Operation(summary = "Iniciar sesión",
            description = "Público. Devuelve el JWT que se envía en el header Authorization: Bearer <token>.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Credenciales correctas: token JWT")
    @ApiResponse(responseCode = "400", description = "Falta el usuario o la contraseña",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
