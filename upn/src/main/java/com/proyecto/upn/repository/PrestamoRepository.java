package com.proyecto.upn.repository;

import com.proyecto.upn.entity.EstadoPrestamo;
import com.proyecto.upn.entity.Prestamo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    @EntityGraph(attributePaths = {"solicitante", "registradoPor", "detalles", "detalles.equipo"})
    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    @EntityGraph(attributePaths = {"solicitante", "registradoPor", "detalles", "detalles.equipo"})
    Optional<Prestamo> findWithDetallesById(Long id);

    boolean existsBySolicitanteId(Long solicitanteId);

    /** ¿El solicitante tiene préstamos activos cuya fecha de devolución ya pasó? */
    boolean existsBySolicitanteIdAndEstadoAndFechaDevolucionPrevistaBefore(Long solicitanteId, EstadoPrestamo estado,
                                                                           LocalDate fecha);
}
