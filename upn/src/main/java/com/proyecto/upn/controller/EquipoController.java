package com.proyecto.upn.controller;

import com.proyecto.upn.dto.EquipoRequest;
import com.proyecto.upn.dto.EquipoResponse;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.entity.Equipo;
import com.proyecto.upn.entity.EstadoEquipo;
import com.proyecto.upn.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Equipos", description = "Inventario de equipos tecnológicos (equivale a \"productos\"). "
        + "Consultar: ADMIN y ENCARGADO · Registrar, editar y eliminar: solo ADMIN")
@RestController
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService equipoService;

    /** GET /api/equipos → 200 (lista completa: la usan los formularios de préstamo) */
    @Operation(summary = "Listar todos los equipos",
            description = "Lista completa sin paginar, ordenada por categoría y nombre. La usa el formulario de "
                    + "préstamos para elegir equipos. Para el listado de la pantalla use /api/equipos/paginado.")
    @ApiResponse(responseCode = "200", description = "Lista de equipos")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<List<Equipo>> index() {
        return ResponseEntity.ok(equipoService.listar());
    }

    /** GET /api/equipos/paginado?page=0&size=10&sort=nombre,asc&busqueda=&estado=&categoria= → 200 | 400 */
    @Operation(summary = "Listar equipos paginados",
            description = "Búsqueda paginada con JPQL por nombre, código o marca (sin distinguir mayúsculas), con "
                    + "filtros opcionales de estado y categoría. Ordenable por: codigo, nombre, categoria, marca, "
                    + "estado, cantidadTotal, cantidadDisponible.")
    @ApiResponse(responseCode = "200", description = "Página de equipos")
    @ApiResponse(responseCode = "400", description = "page < 0, size fuera de 1..50, campo de orden no permitido o estado inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/paginado")
    public ResponseEntity<PageResponse<EquipoResponse>> paginado(
            @Parameter(description = "Número de página, empieza en 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser mayor o igual a 0") Integer page,
            @Parameter(description = "Registros por página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 50, message = "size no puede ser mayor a 50") Integer size,
            @Parameter(description = "Orden: campo,asc|desc", example = "nombre,asc")
            @RequestParam(defaultValue = EquipoService.ORDEN_POR_DEFECTO) String sort,
            @Parameter(description = "Texto a buscar en nombre, código o marca", example = "laptop")
            @RequestParam(required = false) String busqueda,
            @Parameter(description = "Filtro por estado")
            @RequestParam(required = false) EstadoEquipo estado,
            @Parameter(description = "Filtro por categoría exacta", example = "Laptops")
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(equipoService.listarPaginado(page, size, sort, busqueda, estado, categoria));
    }

    /** GET /api/equipos/disponibles → 200 */
    @Operation(summary = "Equipos disponibles para préstamo",
            description = "Consulta JPQL: equipos en estado OPERATIVO con cantidadDisponible > 0.")
    @ApiResponse(responseCode = "200", description = "Equipos prestables")
    @GetMapping("/disponibles")
    public ResponseEntity<List<EquipoResponse>> disponibles() {
        return ResponseEntity.ok(equipoService.listarPrestables());
    }

    /** GET /api/equipos/{id} → 200 | 404 */
    @Operation(summary = "Obtener un equipo")
    @ApiResponse(responseCode = "200", description = "Equipo encontrado")
    @ApiResponse(responseCode = "404", description = "El equipo no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<Equipo> show(@Parameter(description = "ID del equipo", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(equipoService.buscar(id));
    }

    /** POST /api/equipos → 201 | 400 | 409 */
    @Operation(summary = "Registrar un equipo (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Equipo creado; el header Location apunta al nuevo recurso")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (detalle por campo en 'errores')",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un equipo con ese código",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<Equipo> create(@Valid @RequestBody EquipoRequest request) {
        Equipo creado = equipoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** PUT /api/equipos/{id} → 200 | 400 | 404 | 409 */
    @Operation(summary = "Actualizar un equipo (ADMIN)",
            description = "Conserva las unidades prestadas: la cantidad total no puede ser menor que ellas.")
    @ApiResponse(responseCode = "200", description = "Equipo actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o cantidad menor a las unidades prestadas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El equipo no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Código duplicado o modificación simultánea (bloqueo optimista)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<Equipo> update(@Parameter(description = "ID del equipo", example = "1") @PathVariable Long id,
                                         @Valid @RequestBody EquipoRequest request) {
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    /** DELETE /api/equipos/{id} → 204 | 404 | 409 */
    @Operation(summary = "Eliminar un equipo (ADMIN)",
            description = "No se elimina si tiene préstamos registrados: en ese caso cambie su estado a DE_BAJA.")
    @ApiResponse(responseCode = "204", description = "Equipo eliminado")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El equipo no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El equipo tiene préstamos registrados",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID del equipo", example = "1") @PathVariable Long id) {
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
