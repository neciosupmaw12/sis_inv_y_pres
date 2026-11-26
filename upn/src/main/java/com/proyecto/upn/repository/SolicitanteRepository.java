package com.proyecto.upn.repository;

import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.entity.TipoSolicitante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {
    Optional<Solicitante> findByDni(String dni);
    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);

    /** Búsqueda paginada por DNI, nombres o apellidos con filtro opcional por tipo de solicitante. */
    @Query(value = """
            SELECT s FROM Solicitante s
            WHERE (:patron IS NULL
                   OR s.dni LIKE :patron
                   OR LOWER(CONCAT(s.nombres, ' ', s.apellidos)) LIKE :patron
                   OR LOWER(s.apellidos) LIKE :patron)
              AND (:tipo IS NULL OR s.tipo = :tipo)
            """,
            countQuery = """
            SELECT COUNT(s) FROM Solicitante s
            WHERE (:patron IS NULL
                   OR s.dni LIKE :patron
                   OR LOWER(CONCAT(s.nombres, ' ', s.apellidos)) LIKE :patron
                   OR LOWER(s.apellidos) LIKE :patron)
              AND (:tipo IS NULL OR s.tipo = :tipo)
            """)
    Page<Solicitante> buscar(@Param("patron") String patron,
                             @Param("tipo") TipoSolicitante tipo,
                             Pageable pageable);
}
