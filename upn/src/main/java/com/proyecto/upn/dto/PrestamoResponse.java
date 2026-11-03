package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Prestamo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PrestamoResponse(
        Long id,
        LocalDateTime fechaPrestamo,
        LocalDate fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal,
        /** ACTIVO, VENCIDO o DEVUELTO */
        String estado,
        Long solicitanteId,
        String solicitante,
        String tipoSolicitante,
        String registradoPor,
        Integer totalEquipos,
        String observaciones,
        List<DetallePrestamoResponse> detalles
) {
    public record DetallePrestamoResponse(Long equipoId, String codigo, String equipo, Integer cantidad) {
    }

    public static PrestamoResponse de(Prestamo p) {
        List<DetallePrestamoResponse> detalles = p.getDetalles().stream()
                .map(d -> new DetallePrestamoResponse(d.getEquipo().getId(), d.getEquipo().getCodigo(),
                        d.getEquipo().getNombre(), d.getCantidad()))
                .toList();
        String estado = p.isVencido() ? "VENCIDO" : p.getEstado().name();
        int total = detalles.stream().mapToInt(DetallePrestamoResponse::cantidad).sum();
        return new PrestamoResponse(p.getId(), p.getFechaPrestamo(), p.getFechaDevolucionPrevista(),
                p.getFechaDevolucionReal(), estado, p.getSolicitante().getId(),
                p.getSolicitante().getNombres() + " " + p.getSolicitante().getApellidos(),
                p.getSolicitante().getTipo().name(), p.getRegistradoPor().getNombre(), total,
                p.getObservaciones(), detalles);
    }
}
