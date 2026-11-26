package com.proyecto.upn.controller;

import com.proyecto.upn.config.EjemplosError;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.SolicitanteRequest;
import com.proyecto.upn.dto.SolicitanteResponse;
import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.entity.TipoSolicitante;
import com.proyecto.upn.service.SolicitanteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Solicitantes", description = "Estudiantes, docentes y administrativos que piden equipos (equivale a "
        + "\"clientes\"). Consultar, registrar y editar: ADMIN y ENCARGADO · Eliminar: solo ADMIN")
@RestController
@RequestMapping(path = "/api/solicitantes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
public class SolicitanteController {

    private final SolicitanteService solicitanteService;

    @Operation(summary = "Listar todos los solicitantes",
            description = "Lista completa sin paginar; la usa el formulario de préstamos.")
    @ApiResponse(responseCode = "200", description = "Lista de solicitantes")
    @GetMapping
    public ResponseEntity<List<Solicitante>> index() {
        return ResponseEntity.ok(solicitanteService.listar());
    }

    @Operation(summary = "Listar solicitantes paginados",
            description = "Búsqueda JPQL por DNI, nombres o apellidos, con filtro opcional por tipo. Ordenable por: "
                    + "apellidos, nombres, dni, tipo, area.")
    @ApiResponse(responseCode = "200", description = "Página de solicitantes")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación u orden no válidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "SOLICITANTES_PAGE_INVALIDO", summary = "page negativo", value = EjemplosError.SOLICITANTES_PAGE_INVALIDO),
                            @ExampleObject(name = "SOLICITANTES_ORDEN_NO_PERMITIDO", summary = "Campo de orden no permitido", value = EjemplosError.SOLICITANTES_ORDEN_NO_PERMITIDO)}))
    @GetMapping("/paginado")
    public ResponseEntity<PageResponse<SolicitanteResponse>> paginado(
            @Parameter(description = "Número de página, empieza en 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser mayor o igual a 0") Integer page,
            @Parameter(description = "Registros por página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 50, message = "size no puede ser mayor a 50") Integer size,
            @Parameter(description = "Orden: campo,asc|desc", example = "apellidos,asc")
            @RequestParam(defaultValue = SolicitanteService.ORDEN_POR_DEFECTO) String sort,
            @Parameter(description = "DNI, nombres o apellidos", example = "quispe")
            @RequestParam(required = false) String busqueda,
            @Parameter(description = "Filtro por tipo de solicitante")
            @RequestParam(required = false) TipoSolicitante tipo) {
        return ResponseEntity.ok(solicitanteService.listarPaginado(page, size, sort, busqueda, tipo));
    }

    @Operation(summary = "Obtener un solicitante")
    @ApiResponse(responseCode = "200", description = "Solicitante encontrado")
    @ApiResponse(responseCode = "404", description = "El solicitante no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_NO_ENCONTRADO", summary = "Solicitante inexistente", value = EjemplosError.SOLICITANTE_NO_ENCONTRADO)))
    @GetMapping("/{id}")
    public ResponseEntity<Solicitante> show(@Parameter(description = "ID del solicitante", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(solicitanteService.buscar(id));
    }

    @Operation(summary = "Registrar un solicitante")
    @ApiResponse(responseCode = "201", description = "Solicitante creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.SOLICITANTE_DATOS_INVALIDOS)))
    @ApiResponse(responseCode = "409", description = "DNI o email duplicado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "SOLICITANTE_DNI_DUPLICADO", summary = "DNI duplicado", value = EjemplosError.SOLICITANTE_DNI_DUPLICADO),
                            @ExampleObject(name = "SOLICITANTE_EMAIL_DUPLICADO", summary = "Email duplicado", value = EjemplosError.SOLICITANTE_EMAIL_DUPLICADO)}))
    @PostMapping
    public ResponseEntity<Solicitante> create(@Valid @RequestBody SolicitanteRequest request) {
        Solicitante creado = solicitanteService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @Operation(summary = "Actualizar un solicitante")
    @ApiResponse(responseCode = "200", description = "Solicitante actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_PUT_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.SOLICITANTE_PUT_DATOS_INVALIDOS)))
    @ApiResponse(responseCode = "404", description = "El solicitante no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_NO_ENCONTRADO", summary = "Solicitante inexistente", value = EjemplosError.SOLICITANTE_NO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "DNI o email duplicado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "SOLICITANTE_PUT_DNI_DUPLICADO", summary = "DNI duplicado", value = EjemplosError.SOLICITANTE_PUT_DNI_DUPLICADO),
                            @ExampleObject(name = "SOLICITANTE_PUT_EMAIL_DUPLICADO", summary = "Email duplicado", value = EjemplosError.SOLICITANTE_PUT_EMAIL_DUPLICADO)}))
    @PutMapping("/{id}")
    public ResponseEntity<Solicitante> update(@Parameter(description = "ID del solicitante", example = "1") @PathVariable Long id,
                                              @Valid @RequestBody SolicitanteRequest request) {
        return ResponseEntity.ok(solicitanteService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un solicitante (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Solicitante eliminado")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_SIN_PERMISO", summary = "Usuario ENCARGADO", value = EjemplosError.SOLICITANTE_SIN_PERMISO)))
    @ApiResponse(responseCode = "404", description = "El solicitante no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_NO_ENCONTRADO", summary = "Solicitante inexistente", value = EjemplosError.SOLICITANTE_NO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "Tiene préstamos registrados",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITANTE_CON_PRESTAMOS", summary = "Tiene préstamos registrados", value = EjemplosError.SOLICITANTE_CON_PRESTAMOS)))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID del solicitante", example = "1") @PathVariable Long id) {
        solicitanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
