package com.proyecto.upn.controller;

import com.proyecto.upn.config.EjemplosError;
import com.proyecto.upn.dto.ConteoPrestamosDto;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
import com.proyecto.upn.service.PrestamoService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Préstamos", description = "Préstamos y devoluciones (equivale a \"ventas\"). Consultar, registrar y "
        + "devolver: ADMIN y ENCARGADO · Editar y anular: solo ADMIN")
@RestController
@RequestMapping(path = "/api/prestamos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @Operation(summary = "Listar todos los préstamos", description = "Lista completa sin paginar, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Lista de préstamos")
    @GetMapping
    public ResponseEntity<List<PrestamoResponse>> index() {
        return ResponseEntity.ok(prestamoService.listar());
    }

    /** GET /api/prestamos/paginado?page=0&size=10&sort=fechaPrestamo,desc&estado=VENCIDO&busqueda= → 200 | 400 */
    @Operation(summary = "Listar préstamos paginados",
            description = "Paginación en dos consultas JPQL para evitar el problema N+1 sin paginar en memoria: "
                    + "(1) IDs de la página con LIMIT/OFFSET; (2) esos préstamos con JOIN FETCH de solicitante, "
                    + "usuario, detalles y equipos. Ordenable por: id, fechaPrestamo, fechaDevolucionPrevista, "
                    + "estado, solicitante.")
    @ApiResponse(responseCode = "200", description = "Página de préstamos")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación, orden o estado no válidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMOS_ESTADO_INVALIDO", summary = "Estado inexistente", value = EjemplosError.PRESTAMOS_ESTADO_INVALIDO),
                            @ExampleObject(name = "PRESTAMOS_PAGE_INVALIDO", summary = "page negativo", value = EjemplosError.PRESTAMOS_PAGE_INVALIDO)}))
    @GetMapping("/paginado")
    public ResponseEntity<PageResponse<PrestamoResponse>> paginado(
            @Parameter(description = "Número de página, empieza en 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser mayor o igual a 0") Integer page,
            @Parameter(description = "Registros por página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 50, message = "size no puede ser mayor a 50") Integer size,
            @Parameter(description = "Orden: campo,asc|desc", example = "fechaPrestamo,desc")
            @RequestParam(defaultValue = PrestamoService.ORDEN_POR_DEFECTO) String sort,
            @Parameter(description = "ACTIVO (dentro de plazo), VENCIDO, DEVUELTO o vacío para todos",
                    schema = @Schema(allowableValues = {"ACTIVO", "VENCIDO", "DEVUELTO"}))
            @RequestParam(required = false) String estado,
            @Parameter(description = "Nombre, apellido o DNI del solicitante", example = "quispe")
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(prestamoService.listarPaginado(page, size, sort, estado, busqueda));
    }

    @Operation(summary = "Contar préstamos por estado",
            description = "Agregación JPQL con SUM(CASE WHEN ...) en una sola consulta. Alimenta las pestañas de filtro.")
    @ApiResponse(responseCode = "200", description = "Totales por estado")
    @GetMapping("/resumen")
    public ResponseEntity<ConteoPrestamosDto> resumen() {
        return ResponseEntity.ok(prestamoService.contarPorEstado());
    }

    @Operation(summary = "Préstamos vencidos de un solicitante",
            description = "Consulta JPQL: préstamos ACTIVOS del solicitante cuya fecha prevista ya pasó. Es la misma "
                    + "regla que bloquea un nuevo préstamo.")
    @ApiResponse(responseCode = "200", description = "Préstamos vencidos (lista vacía si no tiene)")
    @ApiResponse(responseCode = "404", description = "El solicitante no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "VENCIDOS_SOLICITANTE_NO_ENCONTRADO", summary = "Solicitante inexistente", value = EjemplosError.VENCIDOS_SOLICITANTE_NO_ENCONTRADO)))
    @GetMapping("/vencidos")
    public ResponseEntity<List<PrestamoResponse>> vencidos(
            @Parameter(description = "ID del solicitante", example = "1", required = true)
            @RequestParam Long solicitanteId) {
        return ResponseEntity.ok(prestamoService.vencidosDeSolicitante(solicitanteId));
    }

    @Operation(summary = "Obtener un préstamo con su detalle")
    @ApiResponse(responseCode = "200", description = "Préstamo encontrado")
    @ApiResponse(responseCode = "404", description = "El préstamo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_NO_ENCONTRADO", summary = "Préstamo inexistente", value = EjemplosError.PRESTAMO_NO_ENCONTRADO)))
    @GetMapping("/{id}")
    public ResponseEntity<PrestamoResponse> show(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.buscar(id));
    }

    /** El usuario que registra el préstamo se toma del JWT. */
    @Operation(summary = "Registrar un préstamo",
            description = "Transacción atómica: descuenta el stock de todos los equipos o de ninguno. Si un equipo no "
                    + "tiene unidades suficientes responde 409 y hace ROLLBACK de todo (no se guarda el préstamo, "
                    + "ni sus detalles, ni se descuenta stock).")
    @ApiResponse(responseCode = "201", description = "Préstamo registrado y stock descontado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, equipo no OPERATIVO o solicitante con préstamos vencidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMO_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.PRESTAMO_DATOS_INVALIDOS),
                            @ExampleObject(name = "PRESTAMO_EQUIPO_NO_OPERATIVO", summary = "Equipo no operativo", value = EjemplosError.PRESTAMO_EQUIPO_NO_OPERATIVO),
                            @ExampleObject(name = "PRESTAMO_SOLICITANTE_CON_VENCIDOS", summary = "Solicitante con vencidos", value = EjemplosError.PRESTAMO_SOLICITANTE_CON_VENCIDOS)}))
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMOS_SIN_TOKEN", summary = "Sin token", value = EjemplosError.PRESTAMOS_SIN_TOKEN)))
    @ApiResponse(responseCode = "404", description = "El solicitante o un equipo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMO_SOLICITANTE_NO_ENCONTRADO", summary = "Solicitante inexistente", value = EjemplosError.PRESTAMO_SOLICITANTE_NO_ENCONTRADO),
                            @ExampleObject(name = "PRESTAMO_EQUIPO_NO_ENCONTRADO", summary = "Equipo inexistente", value = EjemplosError.PRESTAMO_EQUIPO_NO_ENCONTRADO)}))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente (rollback) o modificación simultánea del mismo equipo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMO_STOCK_INSUFICIENTE", summary = "Stock insuficiente (rollback)", value = EjemplosError.PRESTAMO_STOCK_INSUFICIENTE),
                            @ExampleObject(name = "PRESTAMO_MODIFICACION_SIMULTANEA", summary = "Bloqueo optimista", value = EjemplosError.PRESTAMO_MODIFICACION_SIMULTANEA)}))
    @PostMapping
    public ResponseEntity<PrestamoResponse> create(@Valid @RequestBody PrestamoRequest request,
                                                   @Parameter(hidden = true) Authentication auth) {
        PrestamoResponse creado = prestamoService.crear(request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @Operation(summary = "Editar un préstamo activo (ADMIN)",
            description = "Repone el stock de los equipos anteriores y descuenta el de los nuevos en la misma transacción.")
    @ApiResponse(responseCode = "200", description = "Préstamo actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o el préstamo ya fue devuelto",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMO_PUT_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.PRESTAMO_PUT_DATOS_INVALIDOS),
                            @ExampleObject(name = "PRESTAMO_NO_EDITABLE", summary = "Préstamo ya devuelto", value = EjemplosError.PRESTAMO_NO_EDITABLE)}))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_SIN_PERMISO", summary = "Usuario ENCARGADO", value = EjemplosError.PRESTAMO_SIN_PERMISO)))
    @ApiResponse(responseCode = "404", description = "El préstamo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_NO_ENCONTRADO", summary = "Préstamo inexistente", value = EjemplosError.PRESTAMO_NO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente (rollback)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_PUT_STOCK_INSUFICIENTE", summary = "Stock insuficiente (rollback)", value = EjemplosError.PRESTAMO_PUT_STOCK_INSUFICIENTE)))
    @PutMapping("/{id}")
    public ResponseEntity<PrestamoResponse> update(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id,
                                                   @Valid @RequestBody PrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.actualizar(id, request));
    }

    /** PUT /api/prestamos/{id}/devolucion → 200 | 400 si ya fue devuelto | 404 */
    @Operation(summary = "Registrar la devolución", description = "Repone el stock de todos los equipos del préstamo en una sola transacción.")
    @ApiResponse(responseCode = "200", description = "Devolución registrada y stock repuesto")
    @ApiResponse(responseCode = "400", description = "El préstamo ya fue devuelto",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_YA_DEVUELTO", summary = "Ya devuelto", value = EjemplosError.PRESTAMO_YA_DEVUELTO)))
    @ApiResponse(responseCode = "404", description = "El préstamo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "DEVOLUCION_PRESTAMO_NO_ENCONTRADO", summary = "Préstamo inexistente", value = EjemplosError.DEVOLUCION_PRESTAMO_NO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "Modificación simultánea del mismo equipo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "DEVOLUCION_MODIFICACION_SIMULTANEA", summary = "Bloqueo optimista", value = EjemplosError.DEVOLUCION_MODIFICACION_SIMULTANEA)))
    @PutMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponse> devolver(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }

    @Operation(summary = "Anular un préstamo (ADMIN)", description = "Si seguía activo, sus equipos vuelven al inventario.")
    @ApiResponse(responseCode = "204", description = "Préstamo anulado")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_SIN_PERMISO", summary = "Usuario ENCARGADO", value = EjemplosError.PRESTAMO_SIN_PERMISO)))
    @ApiResponse(responseCode = "404", description = "El préstamo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_NO_ENCONTRADO", summary = "Préstamo inexistente", value = EjemplosError.PRESTAMO_NO_ENCONTRADO)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id) {
        prestamoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
