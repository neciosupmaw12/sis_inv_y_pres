package com.proyecto.upn.repository;

import com.proyecto.upn.dto.EquipoMasPrestadoDto;
import com.proyecto.upn.dto.EquiposPorEstadoDto;
import com.proyecto.upn.entity.Equipo;
import com.proyecto.upn.entity.EstadoEquipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /**
     * (a) Búsqueda paginada por nombre, código o marca (sin distinguir mayúsculas) con filtros opcionales de estado y
     * categoría. Un parámetro en null desactiva su filtro. {@code patron} llega en minúsculas y con comodines
     * ("%lap%"). La {@code countQuery} calcula el total sin ORDER BY ni LIMIT, para armar totalElements y totalPages.
     */
    @Query(value = """
            SELECT e FROM Equipo e
            WHERE (:patron IS NULL
                   OR LOWER(e.nombre) LIKE :patron
                   OR LOWER(e.codigo) LIKE :patron
                   OR LOWER(e.marca) LIKE :patron)
              AND (:estado IS NULL OR e.estado = :estado)
              AND (:categoria IS NULL OR e.categoria = :categoria)
            """,
            countQuery = """
            SELECT COUNT(e) FROM Equipo e
            WHERE (:patron IS NULL
                   OR LOWER(e.nombre) LIKE :patron
                   OR LOWER(e.codigo) LIKE :patron
                   OR LOWER(e.marca) LIKE :patron)
              AND (:estado IS NULL OR e.estado = :estado)
              AND (:categoria IS NULL OR e.categoria = :categoria)
            """)
    Page<Equipo> buscar(@Param("patron") String patron,
                        @Param("estado") EstadoEquipo estado,
                        @Param("categoria") String categoria,
                        Pageable pageable);

    /** (b) Equipos que se pueden prestar ahora: estado OPERATIVO y al menos una unidad en almacén. */
    @Query("""
            SELECT e FROM Equipo e
            WHERE e.cantidadDisponible > 0 AND e.estado = :estado
            ORDER BY e.categoria, e.nombre
            """)
    List<Equipo> findPrestables(@Param("estado") EstadoEquipo estado);

    /** (e) Agregación para el panel: cantidad de equipos y unidades por estado (proyección con SELECT new). */
    @Query("""
            SELECT new com.proyecto.upn.dto.EquiposPorEstadoDto(
                       e.estado, COUNT(e), SUM(e.cantidadTotal), SUM(e.cantidadDisponible))
            FROM Equipo e
            GROUP BY e.estado
            ORDER BY e.estado
            """)
    List<EquiposPorEstadoDto> contarPorEstado();

    /** (e) Los equipos con más unidades prestadas en el historial; el límite (top 5) llega en el Pageable. */
    @Query("""
            SELECT new com.proyecto.upn.dto.EquipoMasPrestadoDto(
                       eq.id, eq.codigo, eq.nombre, SUM(d.cantidad), COUNT(DISTINCT p.id))
            FROM DetallePrestamo d
            JOIN d.equipo eq
            JOIN d.prestamo p
            GROUP BY eq.id, eq.codigo, eq.nombre
            ORDER BY SUM(d.cantidad) DESC, eq.nombre
            """)
    List<EquipoMasPrestadoDto> masPrestados(Pageable limite);
}
