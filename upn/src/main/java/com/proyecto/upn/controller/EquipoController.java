package com.proyecto.upn.controller;

import com.proyecto.upn.dto.EquipoRequest;
import com.proyecto.upn.entity.Equipo;
import com.proyecto.upn.service.EquipoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService equipoService;

    /** GET /api/equipos → 200 */
    @GetMapping
    public ResponseEntity<List<Equipo>> index() {
        return ResponseEntity.ok(equipoService.listar());
    }

    /** GET /api/equipos/{id} → 200 | 404 */
    @GetMapping("/{id}")
    public ResponseEntity<Equipo> show(@PathVariable Long id) {
        return ResponseEntity.ok(equipoService.buscar(id));
    }

    /** POST /api/equipos → 201 | 400 | 409 */
    @PostMapping
    public ResponseEntity<Equipo> create(@Valid @RequestBody EquipoRequest request) {
        Equipo creado = equipoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** PUT /api/equipos/{id} → 200 | 400 | 404 | 409 */
    @PutMapping("/{id}")
    public ResponseEntity<Equipo> update(@PathVariable Long id, @Valid @RequestBody EquipoRequest request) {
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    /** DELETE /api/equipos/{id} → 204 | 404 | 409 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
