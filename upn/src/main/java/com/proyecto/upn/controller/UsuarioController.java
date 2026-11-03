package com.proyecto.upn.controller;

import com.proyecto.upn.dto.RegistroRequest;
import com.proyecto.upn.dto.UsuarioResponse;
import com.proyecto.upn.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administración de usuarios del sistema (solo rol ADMIN). */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> index() {
        return ResponseEntity.ok(authService.listar());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** Desactiva (borrado lógico) al usuario. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        authService.eliminar(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
