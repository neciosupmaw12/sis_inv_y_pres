package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Prestamo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Préstamo (o solicitud de un alumno) con su detalle de equipos")
public record PrestamoResponse(
        Long id,
        LocalDateTime fechaPrestamo,
        LocalDate fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal,
        @Schema(description = "PENDIENTE, ACTIVO, VENCIDO, DEVUELTO, RECHAZADO o CANCELADO", example = "ACTIVO")
        String estado,
        Long solicitanteId,
        String solicitante,
        String tipoSolicitante,
        String registradoPor,
        Integer totalEquipos,
        @Schema(description = "Observaciones del préstamo directo o motivo de la solicitud del alumno")
        String observaciones,
        @Schema(description = "Solo solicitudes de alumnos: cuándo se enviaron") LocalDateTime fechaSolicitud,
        @Schema(description = "ADMIN que aprobó o rechazó la solicitud", example = "Administrador") String aprobadoPor,
        @Schema(description = "Cuándo se aprobó o rechazó") LocalDateTime fechaAprobacion,
        @Schema(description = "Motivo del rechazo", example = "No hay proyectores para esa fecha") String motivoRechazo,
        List<DetallePrestamoResponse> detalles
) {
    @Schema(description = "Equipo y cantidad del préstamo; 'disponibles' es el stock actual del equipo")
    public record DetallePrestamoResponse(Long equipoId, String codigo, String equipo, Integer cantidad,
                                          @Schema(example = "4") Integer disponibles,
                                          @Schema(example = "OPERATIVO") String estadoEquipo) {
    }

    public static PrestamoResponse de(Prestamo p) {
        List<DetallePrestamoResponse> detalles = p.getDetalles().stream()
                .map(d -> new DetallePrestamoResponse(d.getEquipo().getId(), d.getEquipo().getCodigo(),
                        d.getEquipo().getNombre(), d.getCantidad(), d.getEquipo().getCantidadDisponible(),
                        d.getEquipo().getEstado().name()))
                .toList();
        String estado = p.isVencido() ? "VENCIDO" : p.getEstado().name();
        int total = detalles.stream().mapToInt(DetallePrestamoResponse::cantidad).sum();
        return new PrestamoResponse(p.getId(), p.getFechaPrestamo(), p.getFechaDevolucionPrevista(),
                p.getFechaDevolucionReal(), estado, p.getSolicitante().getId(),
                p.getSolicitante().getNombres() + " " + p.getSolicitante().getApellidos(),
                p.getSolicitante().getTipo().name(), p.getRegistradoPor().getNombre(), total,
                p.getObservaciones(), p.getFechaSolicitud(),
                p.getAprobadoPor() == null ? null : p.getAprobadoPor().getNombre(),
                p.getFechaAprobacion(), p.getMotivoRechazo(), detalles);
    }
}
