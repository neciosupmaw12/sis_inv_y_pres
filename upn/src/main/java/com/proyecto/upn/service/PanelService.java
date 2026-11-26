package com.proyecto.upn.service;

import com.proyecto.upn.dto.EquiposPorEstadoDto;
import com.proyecto.upn.dto.PanelResumenResponse;
import com.proyecto.upn.entity.EstadoEquipo;
import com.proyecto.upn.entity.EstadoPrestamo;
import com.proyecto.upn.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Indicadores del panel de inicio calculados en la base de datos (consultas JPQL de agregación). */
@Service
@RequiredArgsConstructor
public class PanelService {

    private static final int TOP_EQUIPOS = 5;

    private final EquipoRepository equipoRepository;
    private final PrestamoService prestamoService;

    @Transactional(readOnly = true)
    public PanelResumenResponse resumen() {
        List<EquiposPorEstadoDto> porEstado = equipoRepository.contarPorEstado();
        long totales = porEstado.stream().mapToLong(EquiposPorEstadoDto::unidadesTotales).sum();
        long disponiblesOperativos = porEstado.stream()
                .filter(e -> e.estado() == EstadoEquipo.OPERATIVO)
                .mapToLong(EquiposPorEstadoDto::unidadesDisponibles).sum();
        long prestadas = porEstado.stream()
                .mapToLong(e -> e.unidadesTotales() - e.unidadesDisponibles()).sum();
        return new PanelResumenResponse(porEstado, totales, disponiblesOperativos, prestadas,
                prestamoService.contarPorEstado(),
                equipoRepository.masPrestados(List.of(EstadoPrestamo.ACTIVO, EstadoPrestamo.DEVUELTO),
                        PageRequest.of(0, TOP_EQUIPOS)));
    }
}
