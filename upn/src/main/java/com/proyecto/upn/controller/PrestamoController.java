package com.proyecto.upn.controller;

import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
import com.proyecto.upn.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @GetMapping
    public ResponseEntity<List<PrestamoResponse>> index() {
        return ResponseEntity.ok(prestamoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoResponse> show(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.buscar(id));
    }

    /** El usuario que registra el préstamo se toma del JWT. */
    @PostMapping
    public ResponseEntity<PrestamoResponse> create(@Valid @RequestBody PrestamoRequest request, Authentication auth) {
        PrestamoResponse creado = prestamoService.crear(request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrestamoResponse> update(@PathVariable Long id, @Valid @RequestBody PrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.actualizar(id, request));
    }

    /** PUT /api/prestamos/{id}/devolucion → 200 | 400 si ya fue devuelto | 404 */
    @PutMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponse> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        prestamoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
