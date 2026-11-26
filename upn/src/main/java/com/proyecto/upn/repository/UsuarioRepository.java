package com.proyecto.upn.repository;

import com.proyecto.upn.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);

    /** Para el login: trae el solicitante vinculado en la misma consulta (su id va en el JWT). */
    @EntityGraph(attributePaths = "solicitante")
    Optional<Usuario> findWithSolicitanteByUsername(String username);

    /** Listado de /usuarios con el estudiante vinculado, sin N+1. */
    @EntityGraph(attributePaths = "solicitante")
    List<Usuario> findAllByOrderByIdAsc();

    Optional<Usuario> findBySolicitanteId(Long solicitanteId);
}
