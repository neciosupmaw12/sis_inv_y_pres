package com.proyecto.upn.service;

import com.proyecto.upn.dto.DetallePrestamoRequest;
import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
import com.proyecto.upn.entity.*;
import com.proyecto.upn.exception.BusinessException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.repository.EquipoRepository;
import com.proyecto.upn.repository.PrestamoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Transactional(readOnly = true)
    public PrestamoResponse buscar(Long id) {
        return PrestamoResponse.de(obtener(id));
    }

    @Transactional
    public PrestamoResponse crear(PrestamoRequest req, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no válido"));
        if (prestamoRepository.existsBySolicitanteIdAndEstadoAndFechaDevolucionPrevistaBefore(
                req.solicitanteId(), EstadoPrestamo.ACTIVO, LocalDate.now())) {
            throw new BusinessException("El solicitante tiene préstamos vencidos; debe devolverlos antes de un nuevo préstamo");
        }
        Prestamo prestamo = new Prestamo();
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setRegistradoPor(usuario);
        aplicar(prestamo, req);
        return PrestamoResponse.de(prestamoRepository.save(prestamo));
    }

    /** Solo un préstamo ACTIVO puede editarse: se devuelven sus equipos y se aplican los nuevos. */
    @Transactional
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

    /** Registra la devolución: los equipos vuelven a estar disponibles. */
    @Transactional
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
    @Transactional
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
                throw new BusinessException("No hay unidades suficientes de '" + equipo.getNombre()
                        + "'. Disponibles: " + equipo.getCantidadDisponible() + ", solicitadas: " + cantidad);
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
