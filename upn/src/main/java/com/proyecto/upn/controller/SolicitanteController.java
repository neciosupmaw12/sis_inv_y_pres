package com.proyecto.upn.controller;

import com.proyecto.upn.dto.SolicitanteRequest;
import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.service.SolicitanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/solicitantes")
@RequiredArgsConstructor
public class SolicitanteController {

    private final SolicitanteService solicitanteService;

    @GetMapping
    public ResponseEntity<List<Solicitante>> index() {
        return ResponseEntity.ok(solicitanteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Solicitante> show(@PathVariable Long id) {
        return ResponseEntity.ok(solicitanteService.buscar(id));
    }

    @PostMapping
    public ResponseEntity<Solicitante> create(@Valid @RequestBody SolicitanteRequest request) {
        Solicitante creado = solicitanteService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Solicitante> update(@PathVariable Long id, @Valid @RequestBody SolicitanteRequest request) {
        return ResponseEntity.ok(solicitanteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        solicitanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
