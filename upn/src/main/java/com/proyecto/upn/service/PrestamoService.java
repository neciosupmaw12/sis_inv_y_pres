package com.proyecto.upn.service;

import com.proyecto.upn.dto.ConteoPrestamosDto;
import com.proyecto.upn.dto.DetallePrestamoRequest;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestamoService {

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
     * Listado paginado en dos consultas JPQL (ver {@link PrestamoRepository#buscarIds}):
     * 1) IDs de la página con LIMIT/OFFSET en MySQL; 2) esos préstamos con JOIN FETCH de sus relaciones.
     *
     * @param estado ACTIVO (vigentes), VENCIDO, DEVUELTO o vacío para todos
     */
    @Transactional(readOnly = true)
    public PageResponse<PrestamoResponse> listarPaginado(Integer page, Integer size, String sort, String estado,
                                                         String busqueda) {
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
            case "DEVUELTO" -> guardado = EstadoPrestamo.DEVUELTO;
            default -> throw new BusinessException("Estado no válido: '" + estado
                    + "'. Use ACTIVO, VENCIDO o DEVUELTO");
        }
        Page<Long> ids = prestamoRepository.buscarIds(guardado, venceAntes, venceDesde,
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

    /** Cantidad de préstamos por estado (para las pestañas de filtro y el panel de inicio). */
    @Transactional(readOnly = true)
    public ConteoPrestamosDto contarPorEstado() {
        return prestamoRepository.contarPorEstado(EstadoPrestamo.ACTIVO, EstadoPrestamo.DEVUELTO, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public PrestamoResponse buscar(Long id) {
        return PrestamoResponse.de(obtener(id));
    }

    /**
     * Registra un préstamo con uno o varios equipos en UNA transacción.
     * Si cualquier equipo falla (no existe, no está OPERATIVO o no tiene stock), se lanza una excepción y Spring hace
     * ROLLBACK: no se guarda el préstamo ni sus detalles y no se descuenta el stock de ningún equipo, aunque los
     * anteriores ya se hubieran procesado en memoria.
     */
    @Transactional(rollbackFor = Exception.class)
    public PrestamoResponse crear(PrestamoRequest req, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no válido"));
        // Regla de bloqueo: consulta JPQL (d) de préstamos vencidos del solicitante
        List<Prestamo> vencidos = prestamoRepository.findVencidosDeSolicitante(req.solicitanteId(),
                EstadoPrestamo.ACTIVO, LocalDate.now());
        if (!vencidos.isEmpty()) {
            String numeros = vencidos.stream().map(p -> "#" + p.getId()).collect(Collectors.joining(", "));
            throw new BusinessException("El solicitante tiene préstamos vencidos (" + numeros
                    + "); debe devolverlos antes de un nuevo préstamo");
        }
        Prestamo prestamo = new Prestamo();
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setRegistradoPor(usuario);
        aplicar(prestamo, req);
        return PrestamoResponse.de(prestamoRepository.save(prestamo));
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
        reponerStock(prestamo);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        return PrestamoResponse.de(prestamo);
    }

    /** Anula el préstamo; si seguía activo, sus equipos vuelven al inventario. */
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
            if (equipo.getEstado() != EstadoEquipo.OPERATIVO) {
                throw new BusinessException("El equipo '" + equipo.getNombre() + "' no está operativo ("
                        + equipo.getEstado() + ")");
            }
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
