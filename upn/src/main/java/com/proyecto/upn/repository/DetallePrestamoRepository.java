package com.proyecto.upn.repository;

import com.proyecto.upn.entity.DetallePrestamo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePrestamoRepository extends JpaRepository<DetallePrestamo, Long> {
    boolean existsByEquipoId(Long equipoId);
}
