package com.proyecto.upn.service;

import com.proyecto.upn.dto.ConteoPrestamosDto;
import com.proyecto.upn.dto.DetallePrestamoRequest;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
import com.proyecto.upn.dto.SolicitudRequest;
import com.proyecto.upn.entity.*;
import com.proyecto.upn.exception.BusinessException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.exception.StockInsuficienteException;
import com.proyecto.upn.repository.EquipoRepository;
import com.proyecto.upn.repository.PrestamoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.repository.UsuarioRepository;
import com.proyecto.upn.util.Paginacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    /** Límites de una solicitud de alumno ("cantidad razonable"). */
    public static final int MAX_UNIDADES_POR_EQUIPO = 3;
    public static final int MAX_UNIDADES_POR_SOLICITUD = 5;
    public static final int MAX_SOLICITUDES_PENDIENTES = 3;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PrestamoRepository prestamoRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<PrestamoResponse> listar() {
        return prestamoRepository.findAllByOrderByFechaPrestamoDesc().stream().map(PrestamoResponse::de).toList();
    }

    public static final Map<String, String> ORDEN_PERMITIDO = Map.of(
            "id", "id",
            "fechaPrestamo", "fechaPrestamo",
            "fechaDevolucionPrevista", "fechaDevolucionPrevista",
            "estado", "estado",
            "solicitante", "solicitante.apellidos");
    public static final String ORDEN_POR_DEFECTO = "fechaPrestamo,desc";

    /**
     * Listado paginado de todos los préstamos (ADMIN y ENCARGADO), en dos consultas JPQL
     * (ver {@link PrestamoRepository#buscarIds}): 1) IDs de la página con LIMIT/OFFSET en MySQL;
     * 2) esos préstamos con JOIN FETCH de sus relaciones.
     *
     * @param estado PENDIENTE, ACTIVO (vigentes), VENCIDO, DEVUELTO, RECHAZADO, CANCELADO o vacío para todos
     */
    @Transactional(readOnly = true)
    public PageResponse<PrestamoResponse> listarPaginado(Integer page, Integer size, String sort, String estado,
                                                         String busqueda) {
        return paginar(page, size, sort, estado, busqueda, null);
    }

    /** "Mis préstamos" del alumno: el mismo listado, filtrado por el solicitante que viene en su JWT. */
    @Transactional(readOnly = true)
    public PageResponse<PrestamoResponse> listarDeSolicitante(Long solicitanteId, Integer page, Integer size,
                                                              String sort, String estado) {
        return paginar(page, size, sort, estado, null, solicitanteId);
    }

    private PageResponse<PrestamoResponse> paginar(Integer page, Integer size, String sort, String estado,
                                                   String busqueda, Long solicitanteId) {
        Pageable pageable = Paginacion.crear(page, size, sort, ORDEN_PERMITIDO, ORDEN_POR_DEFECTO);
        LocalDate hoy = LocalDate.now();
        EstadoPrestamo guardado = null;
        LocalDate venceAntes = null;
        LocalDate venceDesde = null;
        String filtro = estado == null ? "" : estado.trim().toUpperCase();
        switch (filtro) {
            case "", "TODOS" -> { }
            case "ACTIVO" -> { guardado = EstadoPrestamo.ACTIVO; venceDesde = hoy; }
            case "VENCIDO" -> { guardado = EstadoPrestamo.ACTIVO; venceAntes = hoy; }
            case "PENDIENTE" -> guardado = EstadoPrestamo.PENDIENTE;
            case "DEVUELTO" -> guardado = EstadoPrestamo.DEVUELTO;
            case "RECHAZADO" -> guardado = EstadoPrestamo.RECHAZADO;
            case "CANCELADO" -> guardado = EstadoPrestamo.CANCELADO;
            default -> throw new BusinessException("Estado no válido: '" + estado
                    + "'. Use PENDIENTE, ACTIVO, VENCIDO, DEVUELTO, RECHAZADO o CANCELADO");
        }
        Page<Long> ids = prestamoRepository.buscarIds(guardado, venceAntes, venceDesde, solicitanteId,
                Paginacion.patron(busqueda), pageable);

        // La segunda consulta no garantiza el orden: se reordena según la lista de IDs de la página
        Map<Long, Prestamo> porId = new HashMap<>();
        if (!ids.isEmpty()) {
            prestamoRepository.findConDetallesByIdIn(ids.getContent()).forEach(p -> porId.put(p.getId(), p));
        }
        List<PrestamoResponse> contenido = ids.getContent().stream()
                .map(porId::get).filter(Objects::nonNull).map(PrestamoResponse::de).toList();
        return PageResponse.of(ids, contenido, Paginacion.normalizarOrden(sort, ORDEN_POR_DEFECTO));
    }

    /** Préstamos vencidos (activos con fecha prevista pasada) de un solicitante. */
    @Transactional(readOnly = true)
    public List<PrestamoResponse> vencidosDeSolicitante(Long solicitanteId) {
        if (!solicitanteRepository.existsById(solicitanteId)) {
            throw new ResourceNotFoundException("Solicitante", solicitanteId);
        }
        List<Long> ids = prestamoRepository.findVencidosDeSolicitante(solicitanteId, EstadoPrestamo.ACTIVO,
                LocalDate.now()).stream().map(Prestamo::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return prestamoRepository.findConDetallesByIdIn(ids).stream()
                .sorted(Comparator.comparing(Prestamo::getFechaDevolucionPrevista))
                .map(PrestamoResponse::de).toList();
    }

    /** Cantidad de préstamos por estado (pestañas de /prestamos, panel de inicio y contador de pendientes). */
    @Transactional(readOnly = true)
    public ConteoPrestamosDto contarPorEstado() {
        return contar(null);
    }

    /** Lo mismo, solo con los préstamos del alumno (su inicio y sus pestañas). */
    @Transactional(readOnly = true)
    public ConteoPrestamosDto contarPorEstadoDeSolicitante(Long solicitanteId) {
        return contar(solicitanteId);
    }

    private ConteoPrestamosDto contar(Long solicitanteId) {
        return prestamoRepository.contarPorEstado(EstadoPrestamo.ACTIVO, EstadoPrestamo.DEVUELTO,
                EstadoPrestamo.PENDIENTE, EstadoPrestamo.RECHAZADO, EstadoPrestamo.CANCELADO, LocalDate.now(),
                solicitanteId);
    }

    /**
     * @param solicitanteDelAlumno id del solicitante del JWT si quien consulta es un ALUMNO (null para ADMIN y
     *                             ENCARGADO). Un alumno solo puede ver sus propios préstamos: si no es suyo, 403.
     */
    @Transactional(readOnly = true)
    public PrestamoResponse buscar(Long id, Long solicitanteDelAlumno) {
        Prestamo prestamo = obtener(id);
        if (solicitanteDelAlumno != null) {
            exigirPropio(prestamo, solicitanteDelAlumno);
        }
        return PrestamoResponse.de(prestamo);
    }

    /**
     * Registra un préstamo directo (ADMIN o ENCARGADO) con uno o varios equipos en UNA transacción: nace ACTIVO y
     * descuenta el stock. Si cualquier equipo falla (no existe, no está OPERATIVO o no tiene stock), se lanza una
     * excepción y Spring hace ROLLBACK: no se guarda el préstamo ni sus detalles y no se descuenta el stock de
     * ningún equipo, aunque los anteriores ya se hubieran procesado en memoria.
     */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse crear(PrestamoRequest req, String username) {
        Usuario usuario = usuarioActual(username);
        verificarSinVencidos(req.solicitanteId());
        Prestamo prestamo = new Prestamo();
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setRegistradoPor(usuario);
        aplicar(prestamo, req);
        return PrestamoResponse.de(prestamoRepository.save(prestamo));
    }

    /**
     * Solicitud de un ALUMNO: queda PENDIENTE y NO descuenta stock. Solo valida que los equipos existan y estén
     * OPERATIVOS, los límites de cantidad (3 por equipo, 5 en total, 3 solicitudes pendientes) y que el alumno no
     * tenga préstamos vencidos. El stock se valida y descuenta al aprobar.
     *
     * @param solicitanteId el del JWT del alumno, nunca uno enviado en el request
     */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse solicitar(SolicitudRequest req, Long solicitanteId, String username) {
        Usuario usuario = usuarioActual(username);
        Solicitante solicitante = solicitanteRepository.findById(solicitanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante", solicitanteId));

        long pendientes = prestamoRepository.countBySolicitanteIdAndEstado(solicitanteId, EstadoPrestamo.PENDIENTE);
        if (pendientes >= MAX_SOLICITUDES_PENDIENTES) {
            throw new BusinessException("Ya tiene " + pendientes + " solicitudes pendientes (máximo "
                    + MAX_SOLICITUDES_PENDIENTES + "); espere a que se atiendan o cancele alguna");
        }
        verificarSinVencidos(solicitanteId);

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        req.detalles().forEach(d -> cantidades.merge(d.equipoId(), d.cantidad(), Integer::sum));
        int total = cantidades.values().stream().mapToInt(Integer::intValue).sum();
        if (total > MAX_UNIDADES_POR_SOLICITUD) {
            throw new BusinessException("Una solicitud puede incluir como máximo " + MAX_UNIDADES_POR_SOLICITUD
                    + " unidades en total (pidió " + total + ")");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Prestamo solicitud = new Prestamo();
        solicitud.setEstado(EstadoPrestamo.PENDIENTE);
        solicitud.setFechaSolicitud(ahora);
        solicitud.setFechaPrestamo(ahora);              // se reemplaza por la fecha de aprobación
        solicitud.setFechaDevolucionPrevista(req.fechaDevolucionPrevista());
        solicitud.setSolicitante(solicitante);
        solicitud.setRegistradoPor(usuario);
        solicitud.setObservaciones(req.motivo().trim());
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Equipo equipo = equipoRepository.findById(e.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipo", e.getKey()));
            if (e.getValue() > MAX_UNIDADES_POR_EQUIPO) {
                throw new BusinessException("Puede pedir como máximo " + MAX_UNIDADES_POR_EQUIPO
                        + " unidades de '" + equipo.getNombre() + "' (pidió " + e.getValue() + ")");
            }
            exigirOperativo(equipo);
            solicitud.agregarDetalle(DetallePrestamo.builder().equipo(equipo).cantidad(e.getValue()).build());
        }
        return PrestamoResponse.de(prestamoRepository.save(solicitud));
    }

    /**
     * El ADMIN aprueba una solicitud PENDIENTE en UNA transacción: vuelve a validar cada equipo, descuenta el stock
     * y la pasa a ACTIVO. Si un equipo ya no tiene unidades suficientes se lanza {@link StockInsuficienteException}
     * (409) y el ROLLBACK revierte todo: no se descuenta nada y la solicitud sigue PENDIENTE. El @Version de Equipo
     * protege contra otra operación que descuente el mismo equipo al mismo tiempo.
     */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse aprobar(Long id, String username) {
        Prestamo prestamo = obtener(id);
        exigirPendiente(prestamo, "aprobar");
        if (prestamo.getFechaDevolucionPrevista().isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha de devolución prevista ("
                    + prestamo.getFechaDevolucionPrevista().format(FECHA) + ") ya pasó; rechace la solicitud");
        }
        verificarSinVencidos(prestamo.getSolicitante().getId());
        for (DetallePrestamo d : prestamo.getDetalles()) {
            Equipo equipo = d.getEquipo();
            exigirOperativo(equipo);
            if (equipo.getCantidadDisponible() < d.getCantidad()) {
                throw new StockInsuficienteException(equipo.getId(), equipo.getCodigo(), equipo.getNombre(),
                        equipo.getCantidadDisponible(), d.getCantidad(), "No se aprobó la solicitud.");
            }
            equipo.setCantidadDisponible(equipo.getCantidadDisponible() - d.getCantidad());
        }
        LocalDateTime ahora = LocalDateTime.now();
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setAprobadoPor(usuarioActual(username));
        prestamo.setFechaAprobacion(ahora);
        prestamo.setFechaPrestamo(ahora);               // el préstamo empieza cuando se aprueba
        return PrestamoResponse.de(prestamo);
    }

    /** El ADMIN rechaza una solicitud PENDIENTE con un motivo obligatorio; el stock no se toca. */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse rechazar(Long id, String motivo, String username) {
        Prestamo prestamo = obtener(id);
        exigirPendiente(prestamo, "rechazar");
        prestamo.setEstado(EstadoPrestamo.RECHAZADO);
        prestamo.setMotivoRechazo(motivo.trim());
        prestamo.setAprobadoPor(usuarioActual(username));
        prestamo.setFechaAprobacion(LocalDateTime.now());
        return PrestamoResponse.de(prestamo);
    }

    /** El alumno cancela SU solicitud mientras siga PENDIENTE; el stock no se toca. */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse cancelar(Long id, Long solicitanteDelAlumno) {
        Prestamo prestamo = obtener(id);
        exigirPropio(prestamo, solicitanteDelAlumno);
        exigirPendiente(prestamo, "cancelar");
        prestamo.setEstado(EstadoPrestamo.CANCELADO);
        return PrestamoResponse.de(prestamo);
    }

    /** Solo un préstamo ACTIVO puede editarse: se devuelven sus equipos y se aplican los nuevos. */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse actualizar(Long id, PrestamoRequest req) {
        Prestamo prestamo = obtener(id);
        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO) {
            throw new BusinessException("Solo se pueden editar préstamos activos");
        }
        reponerStock(prestamo);
        prestamo.getDetalles().clear();
        aplicar(prestamo, req);
        return PrestamoResponse.de(prestamoRepository.save(prestamo));
    }

    /** Registra la devolución: los equipos vuelven a estar disponibles (atómico: todo o nada). */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse devolver(Long id) {
        Prestamo prestamo = obtener(id);
        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessException("El préstamo #" + id + " ya fue devuelto");
        }
        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO) {
            throw new BusinessException("Solo se pueden devolver préstamos activos; el #" + id + " está "
                    + prestamo.getEstado());
        }
        reponerStock(prestamo);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        return PrestamoResponse.de(prestamo);
    }

    /** Anula el préstamo; si seguía activo, sus equipos vuelven al inventario (las solicitudes no tocan stock). */
    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long id) {
        Prestamo prestamo = obtener(id);
        if (prestamo.getEstado() == EstadoPrestamo.ACTIVO) {
            reponerStock(prestamo);
        }
        prestamoRepository.delete(prestamo);
    }

    private Prestamo obtener(Long id) {
        return prestamoRepository.findWithDetallesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo", id));
    }

    private Usuario usuarioActual(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no válido"));
    }

    /** Un alumno que intenta ver o cancelar un préstamo ajeno recibe 403. */
    private void exigirPropio(Prestamo prestamo, Long solicitanteDelAlumno) {
        if (!prestamo.getSolicitante().getId().equals(solicitanteDelAlumno)) {
            throw new AccessDeniedException("El préstamo #" + prestamo.getId() + " no pertenece al alumno");
        }
    }

    private void exigirPendiente(Prestamo prestamo, String accion) {
        if (prestamo.getEstado() != EstadoPrestamo.PENDIENTE) {
            throw new BusinessException("Solo se pueden " + accion + " solicitudes PENDIENTES; la #"
                    + prestamo.getId() + " está " + prestamo.getEstado());
        }
    }

    private void exigirOperativo(Equipo equipo) {
        if (equipo.getEstado() != EstadoEquipo.OPERATIVO) {
            throw new BusinessException("El equipo '" + equipo.getNombre() + "' no está operativo ("
                    + equipo.getEstado() + ")");
        }
    }

    /** Regla de bloqueo: consulta JPQL (d) de préstamos vencidos del solicitante. */
    private void verificarSinVencidos(Long solicitanteId) {
        List<Prestamo> vencidos = prestamoRepository.findVencidosDeSolicitante(solicitanteId,
                EstadoPrestamo.ACTIVO, LocalDate.now());
        if (!vencidos.isEmpty()) {
            String numeros = vencidos.stream().map(p -> "#" + p.getId()).collect(Collectors.joining(", "));
            throw new BusinessException("El solicitante tiene préstamos vencidos (" + numeros
                    + "); debe devolverlos antes de un nuevo préstamo");
        }
    }

    /** Asigna solicitante, fecha y equipos al préstamo, descontando unidades disponibles. */
    private void aplicar(Prestamo prestamo, PrestamoRequest req) {
        Long solicitanteId = req.solicitanteId();
        Solicitante solicitante = solicitanteRepository.findById(solicitanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante", solicitanteId));
        prestamo.setSolicitante(solicitante);
        prestamo.setFechaDevolucionPrevista(req.fechaDevolucionPrevista());
        prestamo.setObservaciones(req.observaciones() == null || req.observaciones().isBlank()
                ? null : req.observaciones().trim());

        // Agrupa líneas repetidas del mismo equipo
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetallePrestamoRequest d : req.detalles()) {
            cantidades.merge(d.equipoId(), d.cantidad(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Equipo equipo = equipoRepository.findById(e.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipo", e.getKey()));
            int cantidad = e.getValue();
            exigirOperativo(equipo);
            if (equipo.getCantidadDisponible() < cantidad) {
                throw new StockInsuficienteException(equipo.getId(), equipo.getCodigo(), equipo.getNombre(),
                        equipo.getCantidadDisponible(), cantidad);
            }
            equipo.setCantidadDisponible(equipo.getCantidadDisponible() - cantidad);
            prestamo.agregarDetalle(DetallePrestamo.builder().equipo(equipo).cantidad(cantidad).build());
        }
    }

    private void reponerStock(Prestamo prestamo) {
        for (DetallePrestamo d : prestamo.getDetalles()) {
            Equipo equipo = d.getEquipo();
            equipo.setCantidadDisponible(equipo.getCantidadDisponible() + d.getCantidad());
        }
    }
}
