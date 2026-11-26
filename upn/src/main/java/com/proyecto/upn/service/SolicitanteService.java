package com.proyecto.upn.service;

import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.SolicitanteRequest;
import com.proyecto.upn.dto.SolicitanteResponse;
import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.entity.TipoSolicitante;
import com.proyecto.upn.exception.ConflictException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.repository.PrestamoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.repository.UsuarioRepository;
import com.proyecto.upn.exception.BusinessException;
import com.proyecto.upn.util.Paginacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SolicitanteService {

    private final SolicitanteRepository solicitanteRepository;
    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Solicitante> listar() {
        return solicitanteRepository.findAll(Sort.by("apellidos", "nombres"));
    }

    public static final Map<String, String> ORDEN_PERMITIDO = Map.of(
            "apellidos", "apellidos",
            "nombres", "nombres",
            "dni", "dni",
            "tipo", "tipo",
            "area", "area");
    public static final String ORDEN_POR_DEFECTO = "apellidos,asc";

    /** Listado paginado con búsqueda por DNI o nombre y filtro opcional por tipo. */
    @Transactional(readOnly = true)
    public PageResponse<SolicitanteResponse> listarPaginado(Integer page, Integer size, String sort, String busqueda,
                                                            TipoSolicitante tipo) {
        Pageable pageable = Paginacion.crear(page, size, sort, ORDEN_PERMITIDO, ORDEN_POR_DEFECTO);
        Page<Solicitante> pagina = solicitanteRepository.buscar(Paginacion.patron(busqueda), tipo, pageable);
        return PageResponse.of(pagina, SolicitanteResponse::de, Paginacion.normalizarOrden(sort, ORDEN_POR_DEFECTO));
    }

    @Transactional(readOnly = true)
    public Solicitante buscar(Long id) {
        return solicitanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante", id));
    }

    @Transactional(rollbackFor = Exception.class)
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

    @Transactional(rollbackFor = Exception.class)
    public Solicitante actualizar(Long id, SolicitanteRequest req) {
        Solicitante solicitante = buscar(id);
        String email = limpiar(req.email());
        if (solicitanteRepository.existsByDniAndIdNot(req.dni(), id)) {
            throw new ConflictException("Ya existe otro solicitante con el DNI " + req.dni());
        }
        if (email != null && solicitanteRepository.existsByEmailAndIdNot(email.toLowerCase(), id)) {
            throw new ConflictException("Ya existe otro solicitante con el email " + email);
        }
        if (req.tipo() != TipoSolicitante.ESTUDIANTE) {
            usuarioRepository.findBySolicitanteId(id).ifPresent(u -> {
                throw new BusinessException("El solicitante está vinculado al usuario '" + u.getUsername()
                        + "' (ALUMNO); su tipo debe seguir siendo ESTUDIANTE");
            });
        }
        copiar(req, solicitante);
        return solicitanteRepository.save(solicitante);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long id) {
        Solicitante solicitante = buscar(id);
        if (prestamoRepository.existsBySolicitanteId(id)) {
            throw new ConflictException("No se puede eliminar el solicitante porque tiene préstamos registrados");
        }
        usuarioRepository.findBySolicitanteId(id).ifPresent(u -> {
            throw new ConflictException("No se puede eliminar el solicitante porque está vinculado al usuario '"
                    + u.getUsername() + "'");
        });
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
