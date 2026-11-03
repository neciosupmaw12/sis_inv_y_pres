package com.proyecto.upn.service;

import com.proyecto.upn.dto.SolicitanteRequest;
import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.exception.ConflictException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.repository.PrestamoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitanteService {

    private final SolicitanteRepository solicitanteRepository;
    private final PrestamoRepository prestamoRepository;

    @Transactional(readOnly = true)
    public List<Solicitante> listar() {
        return solicitanteRepository.findAll(Sort.by("apellidos", "nombres"));
    }

    @Transactional(readOnly = true)
    public Solicitante buscar(Long id) {
        return solicitanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante", id));
    }

    @Transactional
    public Solicitante crear(SolicitanteRequest req) {
        String email = limpiar(req.email());
        if (solicitanteRepository.existsByDni(req.dni())) {
            throw new ConflictException("Ya existe un solicitante con el DNI " + req.dni());
        }
        if (email != null && solicitanteRepository.existsByEmail(email.toLowerCase())) {
            throw new ConflictException("Ya existe un solicitante con el email " + email);
        }
        Solicitante solicitante = new Solicitante();
        copiar(req, solicitante);
        return solicitanteRepository.save(solicitante);
    }

    @Transactional
    public Solicitante actualizar(Long id, SolicitanteRequest req) {
        Solicitante solicitante = buscar(id);
        String email = limpiar(req.email());
        if (solicitanteRepository.existsByDniAndIdNot(req.dni(), id)) {
            throw new ConflictException("Ya existe otro solicitante con el DNI " + req.dni());
        }
        if (email != null && solicitanteRepository.existsByEmailAndIdNot(email.toLowerCase(), id)) {
            throw new ConflictException("Ya existe otro solicitante con el email " + email);
        }
        copiar(req, solicitante);
        return solicitanteRepository.save(solicitante);
    }

    @Transactional
    public void eliminar(Long id) {
        Solicitante solicitante = buscar(id);
        if (prestamoRepository.existsBySolicitanteId(id)) {
            throw new ConflictException("No se puede eliminar el solicitante porque tiene préstamos registrados");
        }
        solicitanteRepository.delete(solicitante);
    }

    private void copiar(SolicitanteRequest req, Solicitante solicitante) {
        solicitante.setDni(req.dni());
        solicitante.setNombres(req.nombres().trim());
        solicitante.setApellidos(req.apellidos().trim());
        solicitante.setTipo(req.tipo());
        solicitante.setArea(limpiar(req.area()));
        String email = limpiar(req.email());
        solicitante.setEmail(email == null ? null : email.toLowerCase());
        solicitante.setTelefono(limpiar(req.telefono()));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
