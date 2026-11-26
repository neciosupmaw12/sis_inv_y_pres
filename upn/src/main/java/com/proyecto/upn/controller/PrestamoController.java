package com.proyecto.upn.controller;

import com.proyecto.upn.config.EjemplosError;
import com.proyecto.upn.dto.ConteoPrestamosDto;
import com.proyecto.upn.dto.ErrorResponse;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
import com.proyecto.upn.dto.RechazoRequest;
import com.proyecto.upn.dto.SolicitudRequest;
import com.proyecto.upn.security.SesionJwt;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Préstamos", description = "Préstamos y devoluciones (equivale a \"ventas\"). Consultar, registrar y "
        + "devolver: ADMIN y ENCARGADO · Editar, anular, aprobar y rechazar: solo ADMIN · "
        + "ALUMNO: solicitar, ver sus préstamos y cancelar sus solicitudes pendientes")
@RestController
@RequestMapping(path = "/api/prestamos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @Operation(summary = "Listar todos los préstamos", description = "Lista completa sin paginar, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Lista de préstamos")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
    @GetMapping
    public ResponseEntity<List<PrestamoResponse>> index() {
        return ResponseEntity.ok(prestamoService.listar());
    }

    /** GET /api/prestamos/paginado?page=0&size=10&sort=fechaPrestamo,desc&estado=VENCIDO&busqueda= → 200 | 400 */
    @Operation(summary = "Listar préstamos paginados",
            description = "Paginación en dos consultas JPQL para evitar el problema N+1 sin paginar en memoria: "
                    + "(1) IDs de la página con LIMIT/OFFSET; (2) esos préstamos con JOIN FETCH de solicitante, "
                    + "usuario, detalles y equipos. Ordenable por: id, fechaPrestamo, fechaDevolucionPrevista, "
                    + "estado, solicitante. Con estado=PENDIENTE devuelve las solicitudes de alumnos por atender.")
    @ApiResponse(responseCode = "200", description = "Página de préstamos")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación, orden o estado no válidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMOS_ESTADO_INVALIDO", summary = "Estado inexistente", value = EjemplosError.PRESTAMOS_ESTADO_INVALIDO),
                            @ExampleObject(name = "PRESTAMOS_PAGE_INVALIDO", summary = "page negativo", value = EjemplosError.PRESTAMOS_PAGE_INVALIDO)}))
    @ApiResponse(responseCode = "403", description = "El rol es ALUMNO (debe usar /mis-prestamos)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMOS_ALUMNO_SIN_PERMISO", summary = "Usuario ALUMNO", value = EjemplosError.PRESTAMOS_ALUMNO_SIN_PERMISO)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
    @GetMapping("/paginado")
    public ResponseEntity<PageResponse<PrestamoResponse>> paginado(
            @Parameter(description = "Número de página, empieza en 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser mayor o igual a 0") Integer page,
            @Parameter(description = "Registros por página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 50, message = "size no puede ser mayor a 50") Integer size,
            @Parameter(description = "Orden: campo,asc|desc", example = "fechaPrestamo,desc")
            @RequestParam(defaultValue = PrestamoService.ORDEN_POR_DEFECTO) String sort,
            @Parameter(description = "Estado (ACTIVO = dentro de plazo) o vacío para todos",
                    schema = @Schema(allowableValues = {"PENDIENTE", "ACTIVO", "VENCIDO", "DEVUELTO", "RECHAZADO", "CANCELADO"}))
            @RequestParam(required = false) String estado,
            @Parameter(description = "Nombre, apellido o DNI del solicitante", example = "quispe")
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(prestamoService.listarPaginado(page, size, sort, estado, busqueda));
    }

    @Operation(summary = "Contar préstamos por estado",
            description = "Agregación JPQL con SUM(CASE WHEN ...) en una sola consulta. Alimenta las pestañas de filtro "
                    + "y el contador de solicitudes pendientes del navbar.")
    @ApiResponse(responseCode = "200", description = "Totales por estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
    @GetMapping("/vencidos")
    public ResponseEntity<List<PrestamoResponse>> vencidos(
            @Parameter(description = "ID del solicitante", example = "1", required = true)
            @RequestParam Long solicitanteId) {
        return ResponseEntity.ok(prestamoService.vencidosDeSolicitante(solicitanteId));
    }

    // ------------------------------------------------------------------ ALUMNO

    /** GET /api/prestamos/mis-prestamos?page=0&size=10&estado=PENDIENTE → solo los del alumno del JWT */
    @Operation(summary = "Mis préstamos (ALUMNO)",
            description = "Préstamos y solicitudes del alumno autenticado, paginados. El solicitante se toma del JWT "
                    + "(claim solicitanteId), nunca de un parámetro: no hay forma de pedir los de otro alumno.")
    @ApiResponse(responseCode = "200", description = "Página de préstamos del alumno")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación, orden o estado no válidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "MIS_PRESTAMOS_ESTADO_INVALIDO", summary = "Estado inexistente", value = EjemplosError.MIS_PRESTAMOS_ESTADO_INVALIDO)))
    @ApiResponse(responseCode = "403", description = "El rol no es ALUMNO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "MIS_PRESTAMOS_SIN_PERMISO", summary = "Usuario ADMIN o ENCARGADO", value = EjemplosError.MIS_PRESTAMOS_SIN_PERMISO)))
    @PreAuthorize("hasRole('ALUMNO')")
    @GetMapping("/mis-prestamos")
    public ResponseEntity<PageResponse<PrestamoResponse>> misPrestamos(
            @Parameter(description = "Número de página, empieza en 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser mayor o igual a 0") Integer page,
            @Parameter(description = "Registros por página (máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 50, message = "size no puede ser mayor a 50") Integer size,
            @Parameter(description = "Orden: campo,asc|desc", example = "fechaPrestamo,desc")
            @RequestParam(defaultValue = PrestamoService.ORDEN_POR_DEFECTO) String sort,
            @Parameter(description = "Estado o vacío para todos",
                    schema = @Schema(allowableValues = {"PENDIENTE", "ACTIVO", "VENCIDO", "DEVUELTO", "RECHAZADO", "CANCELADO"}))
            @RequestParam(required = false) String estado,
            @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(prestamoService.listarDeSolicitante(SesionJwt.solicitanteId(auth), page, size,
                sort, estado));
    }

    @Operation(summary = "Resumen de mis préstamos (ALUMNO)",
            description = "La misma agregación SUM(CASE WHEN ...) de /resumen, solo con los préstamos del alumno del "
                    + "JWT. Alimenta su pantalla de inicio y sus pestañas.")
    @ApiResponse(responseCode = "200", description = "Totales por estado del alumno")
    @PreAuthorize("hasRole('ALUMNO')")
    @GetMapping("/mis-prestamos/resumen")
    public ResponseEntity<ConteoPrestamosDto> misPrestamosResumen(@Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(prestamoService.contarPorEstadoDeSolicitante(SesionJwt.solicitanteId(auth)));
    }

    /** POST /api/prestamos/solicitudes → 201 PENDIENTE | 400 | 403 | 404 */
    @Operation(summary = "Solicitar un préstamo (ALUMNO)",
            description = "Crea una solicitud PENDIENTE a nombre del alumno del JWT. NO descuenta stock: solo valida "
                    + "que los equipos estén OPERATIVOS, máx. 3 unidades por equipo y 5 por solicitud, máx. 3 "
                    + "solicitudes pendientes y que el alumno no tenga préstamos vencidos. El stock se valida y "
                    + "descuenta cuando el ADMIN la aprueba.")
    @ApiResponse(responseCode = "201", description = "Solicitud registrada en estado PENDIENTE")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, límite de cantidad, equipo no OPERATIVO, "
            + "demasiadas solicitudes pendientes o préstamos vencidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "SOLICITUD_DATOS_INVALIDOS", summary = "Campos inválidos", value = EjemplosError.SOLICITUD_DATOS_INVALIDOS),
                            @ExampleObject(name = "SOLICITUD_EXCEDE_TOTAL", summary = "Más de 5 unidades", value = EjemplosError.SOLICITUD_EXCEDE_TOTAL),
                            @ExampleObject(name = "SOLICITUD_MAX_PENDIENTES", summary = "Ya tiene 3 pendientes", value = EjemplosError.SOLICITUD_MAX_PENDIENTES),
                            @ExampleObject(name = "SOLICITUD_EQUIPO_NO_OPERATIVO", summary = "Equipo no operativo", value = EjemplosError.SOLICITUD_EQUIPO_NO_OPERATIVO),
                            @ExampleObject(name = "SOLICITUD_CON_VENCIDOS", summary = "Alumno con vencidos", value = EjemplosError.SOLICITUD_CON_VENCIDOS)}))
    @ApiResponse(responseCode = "403", description = "El rol no es ALUMNO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITUD_SIN_PERMISO", summary = "Usuario ADMIN o ENCARGADO", value = EjemplosError.SOLICITUD_SIN_PERMISO)))
    @ApiResponse(responseCode = "404", description = "Un equipo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "SOLICITUD_EQUIPO_NO_ENCONTRADO", summary = "Equipo inexistente", value = EjemplosError.SOLICITUD_EQUIPO_NO_ENCONTRADO)))
    @PreAuthorize("hasRole('ALUMNO')")
    @PostMapping("/solicitudes")
    public ResponseEntity<PrestamoResponse> solicitar(@Valid @RequestBody SolicitudRequest request,
                                                      @Parameter(hidden = true) Authentication auth) {
        PrestamoResponse creada = prestamoService.solicitar(request, SesionJwt.solicitanteId(auth), auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/prestamos/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /** PUT /api/prestamos/{id}/cancelar → 200 CANCELADO | 400 si no está PENDIENTE | 403 si es ajena | 404 */
    @Operation(summary = "Cancelar mi solicitud (ALUMNO)",
            description = "Solo la propia y solo mientras siga PENDIENTE. Pasa a CANCELADO; el stock no se toca.")
    @ApiResponse(responseCode = "200", description = "Solicitud cancelada")
    @ApiResponse(responseCode = "400", description = "La solicitud ya fue atendida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "CANCELAR_NO_PENDIENTE", summary = "Ya no está pendiente", value = EjemplosError.CANCELAR_NO_PENDIENTE)))
    @ApiResponse(responseCode = "403", description = "La solicitud es de otro alumno, o el rol no es ALUMNO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "CANCELAR_AJENA", summary = "Solicitud de otro alumno", value = EjemplosError.CANCELAR_AJENA)))
    @ApiResponse(responseCode = "404", description = "La solicitud no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "CANCELAR_NO_ENCONTRADA", summary = "Solicitud inexistente", value = EjemplosError.CANCELAR_NO_ENCONTRADA)))
    @PreAuthorize("hasRole('ALUMNO')")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PrestamoResponse> cancelar(@Parameter(description = "ID de la solicitud", example = "7") @PathVariable Long id,
                                                     @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(prestamoService.cancelar(id, SesionJwt.solicitanteId(auth)));
    }

    // ------------------------------------------------------------------ ADMIN: aprobar / rechazar

    /** PUT /api/prestamos/{id}/aprobar → 200 ACTIVO | 400 | 403 | 404 | 409 stock (rollback) */
    @Operation(summary = "Aprobar una solicitud (ADMIN)",
            description = "Transacción atómica: vuelve a validar cada equipo, descuenta el stock y pasa la solicitud a "
                    + "ACTIVO. Si un equipo ya no tiene unidades suficientes responde 409 y hace ROLLBACK de todo: no "
                    + "se descuenta nada y la solicitud sigue PENDIENTE.")
    @ApiResponse(responseCode = "200", description = "Solicitud aprobada: préstamo ACTIVO y stock descontado")
    @ApiResponse(responseCode = "400", description = "No está PENDIENTE, la fecha de devolución ya pasó, equipo no "
            + "OPERATIVO o el alumno tiene vencidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "APROBAR_NO_PENDIENTE", summary = "Ya fue atendida", value = EjemplosError.APROBAR_NO_PENDIENTE),
                            @ExampleObject(name = "APROBAR_FECHA_VENCIDA", summary = "Fecha de devolución pasada", value = EjemplosError.APROBAR_FECHA_VENCIDA)}))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "APROBAR_SIN_PERMISO", summary = "Usuario ENCARGADO o ALUMNO", value = EjemplosError.APROBAR_SIN_PERMISO)))
    @ApiResponse(responseCode = "404", description = "La solicitud no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "APROBAR_NO_ENCONTRADA", summary = "Solicitud inexistente", value = EjemplosError.APROBAR_NO_ENCONTRADA)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente (rollback) o modificación simultánea del mismo equipo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "APROBAR_STOCK_INSUFICIENTE", summary = "Stock insuficiente (rollback)", value = EjemplosError.APROBAR_STOCK_INSUFICIENTE),
                            @ExampleObject(name = "APROBAR_MODIFICACION_SIMULTANEA", summary = "Bloqueo optimista", value = EjemplosError.APROBAR_MODIFICACION_SIMULTANEA)}))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<PrestamoResponse> aprobar(@Parameter(description = "ID de la solicitud", example = "7") @PathVariable Long id,
                                                    @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(prestamoService.aprobar(id, auth.getName()));
    }

    /** PUT /api/prestamos/{id}/rechazar {"motivo": "..."} → 200 RECHAZADO | 400 | 403 | 404 */
    @Operation(summary = "Rechazar una solicitud (ADMIN)",
            description = "Motivo obligatorio. Pasa a RECHAZADO, guarda quién la rechazó y cuándo; el stock no se toca.")
    @ApiResponse(responseCode = "200", description = "Solicitud rechazada")
    @ApiResponse(responseCode = "400", description = "Falta el motivo o la solicitud no está PENDIENTE",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "RECHAZO_SIN_MOTIVO", summary = "Sin motivo", value = EjemplosError.RECHAZO_SIN_MOTIVO),
                            @ExampleObject(name = "RECHAZAR_NO_PENDIENTE", summary = "Ya fue atendida", value = EjemplosError.RECHAZAR_NO_PENDIENTE)}))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMIN",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "RECHAZAR_SIN_PERMISO", summary = "Usuario ENCARGADO o ALUMNO", value = EjemplosError.RECHAZAR_SIN_PERMISO)))
    @ApiResponse(responseCode = "404", description = "La solicitud no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "RECHAZAR_NO_ENCONTRADA", summary = "Solicitud inexistente", value = EjemplosError.RECHAZAR_NO_ENCONTRADA)))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}/rechazar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrestamoResponse> rechazar(@Parameter(description = "ID de la solicitud", example = "7") @PathVariable Long id,
                                                     @Valid @RequestBody RechazoRequest request,
                                                     @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(prestamoService.rechazar(id, request.motivo(), auth.getName()));
    }

    // ------------------------------------------------------------------ ver, registrar, editar, devolver, anular

    @Operation(summary = "Obtener un préstamo con su detalle",
            description = "ADMIN y ENCARGADO ven cualquiera; un ALUMNO solo los suyos (si pide uno ajeno, 403).")
    @ApiResponse(responseCode = "200", description = "Préstamo encontrado")
    @ApiResponse(responseCode = "403", description = "Un ALUMNO pidió el préstamo de otro solicitante",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_AJENO", summary = "Préstamo de otro alumno", value = EjemplosError.PRESTAMO_AJENO)))
    @ApiResponse(responseCode = "404", description = "El préstamo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "PRESTAMO_NO_ENCONTRADO", summary = "Préstamo inexistente", value = EjemplosError.PRESTAMO_NO_ENCONTRADO)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO', 'ALUMNO')")
    @GetMapping("/{id}")
    public ResponseEntity<PrestamoResponse> show(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id,
                                                 @Parameter(hidden = true) Authentication auth) {
        Long delAlumno = SesionJwt.esAlumno(auth) ? SesionJwt.solicitanteId(auth) : null;
        return ResponseEntity.ok(prestamoService.buscar(id, delAlumno));
    }

    /** El usuario que registra el préstamo se toma del JWT. */
    @Operation(summary = "Registrar un préstamo directo (ADMIN y ENCARGADO)",
            description = "Nace ACTIVO. Transacción atómica: descuenta el stock de todos los equipos o de ninguno. Si "
                    + "un equipo no tiene unidades suficientes responde 409 y hace ROLLBACK de todo (no se guarda el "
                    + "préstamo, ni sus detalles, ni se descuenta stock).")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PrestamoResponse> update(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id,
                                                   @Valid @RequestBody PrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.actualizar(id, request));
    }

    /** PUT /api/prestamos/{id}/devolucion → 200 | 400 si no está ACTIVO | 404 */
    @Operation(summary = "Registrar la devolución", description = "Solo préstamos ACTIVOS (o vencidos). Repone el stock de "
            + "todos los equipos del préstamo en una sola transacción.")
    @ApiResponse(responseCode = "200", description = "Devolución registrada y stock repuesto")
    @ApiResponse(responseCode = "400", description = "El préstamo ya fue devuelto o no está ACTIVO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = {
                            @ExampleObject(name = "PRESTAMO_YA_DEVUELTO", summary = "Ya devuelto", value = EjemplosError.PRESTAMO_YA_DEVUELTO),
                            @ExampleObject(name = "DEVOLUCION_NO_ACTIVO", summary = "Solicitud pendiente", value = EjemplosError.DEVOLUCION_NO_ACTIVO)}))
    @ApiResponse(responseCode = "404", description = "El préstamo no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "DEVOLUCION_PRESTAMO_NO_ENCONTRADO", summary = "Préstamo inexistente", value = EjemplosError.DEVOLUCION_PRESTAMO_NO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "Modificación simultánea del mismo equipo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "DEVOLUCION_MODIFICACION_SIMULTANEA", summary = "Bloqueo optimista", value = EjemplosError.DEVOLUCION_MODIFICACION_SIMULTANEA)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ENCARGADO')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "ID del préstamo", example = "2") @PathVariable Long id) {
        prestamoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
