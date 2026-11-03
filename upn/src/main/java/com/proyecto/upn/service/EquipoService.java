package com.proyecto.upn.service;

import com.proyecto.upn.dto.EquipoRequest;
import com.proyecto.upn.entity.Equipo;
import com.proyecto.upn.exception.BusinessException;
import com.proyecto.upn.exception.ConflictException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.repository.DetallePrestamoRepository;
import com.proyecto.upn.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final DetallePrestamoRepository detallePrestamoRepository;

    @Transactional(readOnly = true)
    public List<Equipo> listar() {
        return equipoRepository.findAll(Sort.by("categoria", "nombre"));
    }

    @Transactional(readOnly = true)
    public Equipo buscar(Long id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", id));
    }

    @Transactional
    public Equipo crear(EquipoRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (equipoRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un equipo con el código " + codigo);
        }
        Equipo equipo = new Equipo();
        copiar(req, equipo);
        equipo.setCantidadDisponible(req.cantidadTotal());
        return equipoRepository.save(equipo);
    }

    @Transactional
    public Equipo actualizar(Long id, EquipoRequest req) {
        Equipo equipo = buscar(id);
        String codigo = req.codigo().trim().toUpperCase();
        if (equipoRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe otro equipo con el código " + codigo);
        }
        // Las unidades prestadas se conservan al cambiar la cantidad total
        int prestadas = equipo.getCantidadTotal() - equipo.getCantidadDisponible();
        if (req.cantidadTotal() < prestadas) {
            throw new BusinessException("La cantidad total no puede ser menor a las " + prestadas
                    + " unidades que están prestadas");
        }
        copiar(req, equipo);
        equipo.setCantidadDisponible(req.cantidadTotal() - prestadas);
        return equipoRepository.save(equipo);
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo equipo = buscar(id);
        if (detallePrestamoRepository.existsByEquipoId(id)) {
            throw new ConflictException("No se puede eliminar el equipo porque tiene préstamos registrados; "
                    + "cambie su estado a DE_BAJA");
        }
        equipoRepository.delete(equipo);
    }

    private void copiar(EquipoRequest req, Equipo equipo) {
        equipo.setCodigo(req.codigo().trim().toUpperCase());
        equipo.setNombre(req.nombre().trim());
        equipo.setCategoria(req.categoria().trim());
        equipo.setMarca(limpiar(req.marca()));
        equipo.setModelo(limpiar(req.modelo()));
        equipo.setDescripcion(limpiar(req.descripcion()));
        equipo.setCantidadTotal(req.cantidadTotal());
        equipo.setEstado(req.estado());
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
