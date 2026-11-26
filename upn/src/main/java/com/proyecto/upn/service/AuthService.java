package com.proyecto.upn.service;

import com.proyecto.upn.dto.AuthResponse;
import com.proyecto.upn.dto.DatosAlumnoRequest;
import com.proyecto.upn.dto.LoginRequest;
import com.proyecto.upn.dto.RegistroRequest;
import com.proyecto.upn.dto.SolicitanteRequest;
import com.proyecto.upn.dto.UsuarioResponse;
import com.proyecto.upn.entity.Rol;
import com.proyecto.upn.entity.Solicitante;
import com.proyecto.upn.entity.TipoSolicitante;
import com.proyecto.upn.entity.Usuario;
import com.proyecto.upn.exception.ConflictException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.exception.ValidacionException;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.repository.UsuarioRepository;
import com.proyecto.upn.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final SolicitanteService solicitanteService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        Usuario usuario = usuarioRepository.findWithSolicitanteByUsername(req.username()).orElseThrow();
        String token = jwtService.generarToken(usuario);
        return new AuthResponse(token, "Bearer", usuario.getUsername(), usuario.getNombre(), usuario.getRol().name(),
                usuario.getSolicitante() == null ? null : usuario.getSolicitante().getId());
    }

    /**
     * Crea un usuario. Para el rol ALUMNO, crear el usuario y su Solicitante es UNA sola operación:
     * <ol>
     *   <li>Se busca el estudiante por DNI: si existe se vincula (sin duplicarlo); si no, se inserta uno nuevo.</li>
     *   <li>Se crea el usuario vinculado a ese estudiante.</li>
     * </ol>
     * Si el paso 2 falla (por ejemplo, el nombre de usuario ya existe → 409), Spring hace ROLLBACK de toda la
     * transacción y el estudiante insertado en el paso 1 tampoco queda guardado.
     */
    @Transactional(rollbackFor = Exception.class)
    public UsuarioResponse registrar(RegistroRequest req) {
        Solicitante estudiante = null;
        String nombre;
        if (req.rol() == Rol.ALUMNO) {
            if (req.alumno() == null) {
                throw new ValidacionException("alumno", "Los datos del estudiante son obligatorios para el rol ALUMNO");
            }
            estudiante = vincularEstudiante(req.alumno());                       // paso 1
            nombre = estudiante.getNombres() + " " + estudiante.getApellidos();
        } else {
            if (req.nombre() == null || req.nombre().isBlank()) {
                throw new ValidacionException("nombre", "El nombre es obligatorio");
            }
            nombre = req.nombre().trim();
        }

        if (usuarioRepository.existsByUsername(req.username())) {              // paso 2
            throw new ConflictException("El usuario " + req.username() + " ya existe");
        }
        Usuario usuario = Usuario.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .nombre(nombre.length() > 100 ? nombre.substring(0, 100) : nombre)
                .rol(req.rol())
                .solicitante(estudiante)
                .activo(true)
                .build();
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /** Devuelve el estudiante con ese DNI (si se puede vincular) o lo crea como ESTUDIANTE. */
    private Solicitante vincularEstudiante(DatosAlumnoRequest d) {
        Optional<Solicitante> existente = solicitanteRepository.findByDni(d.dni());
        if (existente.isPresent()) {
            Solicitante s = existente.get();
            if (s.getTipo() != TipoSolicitante.ESTUDIANTE) {
                throw new ConflictException("El DNI " + d.dni() + " pertenece a un solicitante " + s.getTipo()
                        + "; solo se puede vincular un ESTUDIANTE");
            }
            usuarioRepository.findBySolicitanteId(s.getId()).ifPresent(u -> {
                throw new ConflictException("El estudiante con DNI " + d.dni() + " ya tiene el usuario '"
                        + u.getUsername() + "'");
            });
            return s;
        }
        // Misma validación y normalización que el alta de solicitantes (email duplicado → 409); se une a esta transacción
        return solicitanteService.crear(new SolicitanteRequest(d.dni(), d.nombres(), d.apellidos(),
                TipoSolicitante.ESTUDIANTE, d.area(), d.email(), d.telefono()));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByIdAsc().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long id, String usernameActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        if (usuario.getUsername().equals(usernameActual)) {
            throw new ConflictException("No puede eliminar su propio usuario");
        }
        // Desactivación lógica: el usuario puede tener préstamos asociados
        usuario.setActivo(false);
    }
}
