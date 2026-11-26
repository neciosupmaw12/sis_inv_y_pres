package com.proyecto.upn.repository;

import com.proyecto.upn.dto.ConteoPrestamosDto;
import com.proyecto.upn.entity.EstadoPrestamo;
import com.proyecto.upn.entity.Prestamo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    @EntityGraph(attributePaths = {"solicitante", "registradoPor", "aprobadoPor", "detalles", "detalles.equipo"})
    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    @EntityGraph(attributePaths = {"solicitante", "registradoPor", "aprobadoPor", "detalles", "detalles.equipo"})
    Optional<Prestamo> findWithDetallesById(Long id);

    boolean existsBySolicitanteId(Long solicitanteId);

    /** Para el tope de solicitudes PENDIENTES por alumno. */
    long countBySolicitanteIdAndEstado(Long solicitanteId, EstadoPrestamo estado);

    /**
     * (c) Paginación en dos consultas — paso 1: solo los IDs de la página.
     * <p>
     * No se usa JOIN FETCH de la colección {@code detalles} junto con LIMIT: Hibernate no puede paginar en SQL una
     * consulta que multiplica filas por cada detalle, así que traería TODOS los préstamos y paginaría en memoria
     * (advertencia HHH90003004). Por eso primero se pagina sobre IDs (LIMIT/OFFSET real en MySQL) y luego
     * {@link #findConDetallesByIdIn} carga esos préstamos con sus relaciones en una sola consulta (sin N+1).
     * <p>
     * Filtros (null = sin filtro): {@code estado} guardado; {@code venceAntes} = solo los vencidos;
     * {@code venceDesde} = solo los vigentes; {@code solicitanteId} = solo los de un alumno ("Mis préstamos");
     * {@code patron} busca por nombre, apellido o DNI del solicitante.
     */
    @Query(value = """
            SELECT p.id FROM Prestamo p JOIN p.solicitante s
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:venceAntes IS NULL OR p.fechaDevolucionPrevista < :venceAntes)
              AND (:venceDesde IS NULL OR p.fechaDevolucionPrevista >= :venceDesde)
              AND (:solicitanteId IS NULL OR s.id = :solicitanteId)
              AND (:patron IS NULL
                   OR LOWER(CONCAT(s.nombres, ' ', s.apellidos)) LIKE :patron
                   OR s.dni LIKE :patron)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Prestamo p JOIN p.solicitante s
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:venceAntes IS NULL OR p.fechaDevolucionPrevista < :venceAntes)
              AND (:venceDesde IS NULL OR p.fechaDevolucionPrevista >= :venceDesde)
              AND (:solicitanteId IS NULL OR s.id = :solicitanteId)
              AND (:patron IS NULL
                   OR LOWER(CONCAT(s.nombres, ' ', s.apellidos)) LIKE :patron
                   OR s.dni LIKE :patron)
            """)
    Page<Long> buscarIds(@Param("estado") EstadoPrestamo estado,
                         @Param("venceAntes") LocalDate venceAntes,
                         @Param("venceDesde") LocalDate venceDesde,
                         @Param("solicitanteId") Long solicitanteId,
                         @Param("patron") String patron,
                         Pageable pageable);

    /**
     * (c) Paso 2: carga los préstamos de la página con JOIN FETCH de solicitante, usuarios, detalles y equipos.
     * Una sola consulta SQL en lugar de 1 + N (una por préstamo) + M (una por detalle).
     */
    @Query("""
            SELECT DISTINCT p FROM Prestamo p
            JOIN FETCH p.solicitante
            JOIN FETCH p.registradoPor
            LEFT JOIN FETCH p.aprobadoPor
            LEFT JOIN FETCH p.detalles d
            LEFT JOIN FETCH d.equipo
            WHERE p.id IN :ids
            """)
    List<Prestamo> findConDetallesByIdIn(@Param("ids") Collection<Long> ids);

    /** (d) Préstamos vencidos de un solicitante: siguen ACTIVOS y su fecha prevista es anterior a hoy. */
    @Query("""
            SELECT p FROM Prestamo p
            WHERE p.solicitante.id = :solicitanteId
              AND p.estado = :estado
              AND p.fechaDevolucionPrevista < :hoy
            ORDER BY p.fechaDevolucionPrevista
            """)
    List<Prestamo> findVencidosDeSolicitante(@Param("solicitanteId") Long solicitanteId,
                                             @Param("estado") EstadoPrestamo estado,
                                             @Param("hoy") LocalDate hoy);

    /**
     * (e) Conteo de préstamos por estado en una sola consulta (CASE WHEN dentro de SUM).
     * Con {@code solicitanteId} cuenta solo los de ese alumno (su inicio y sus pestañas); con null, todos.
     */
    @Query("""
            SELECT new com.proyecto.upn.dto.ConteoPrestamosDto(
                COUNT(p),
                COALESCE(SUM(CASE WHEN p.estado = :activo AND p.fechaDevolucionPrevista >= :hoy THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.estado = :activo AND p.fechaDevolucionPrevista < :hoy THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.estado = :devuelto THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.estado = :pendiente THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.estado = :rechazado THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.estado = :cancelado THEN 1 ELSE 0 END), 0))
            FROM Prestamo p
            WHERE (:solicitanteId IS NULL OR p.solicitante.id = :solicitanteId)
            """)
    ConteoPrestamosDto contarPorEstado(@Param("activo") EstadoPrestamo activo,
                                       @Param("devuelto") EstadoPrestamo devuelto,
                                       @Param("pendiente") EstadoPrestamo pendiente,
                                       @Param("rechazado") EstadoPrestamo rechazado,
                                       @Param("cancelado") EstadoPrestamo cancelado,
                                       @Param("hoy") LocalDate hoy,
                                       @Param("solicitanteId") Long solicitanteId);
}
