package com.proyecto.upn.service;

import com.proyecto.upn.dto.AuthResponse;
import com.proyecto.upn.dto.LoginRequest;
import com.proyecto.upn.dto.RegistroRequest;
import com.proyecto.upn.dto.UsuarioResponse;
import com.proyecto.upn.entity.Usuario;
import com.proyecto.upn.exception.ConflictException;
import com.proyecto.upn.exception.ResourceNotFoundException;
import com.proyecto.upn.repository.UsuarioRepository;
import com.proyecto.upn.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        Usuario usuario = usuarioRepository.findByUsername(req.username()).orElseThrow();
        String token = jwtService.generarToken(usuario);
        return new AuthResponse(token, "Bearer", usuario.getUsername(), usuario.getNombre(), usuario.getRol().name());
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        if (usuarioRepository.existsByUsername(req.username())) {
            throw new ConflictException("El usuario " + req.username() + " ya existe");
        }
        Usuario usuario = Usuario.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .nombre(req.nombre().trim())
                .rol(req.rol())
                .activo(true)
                .build();
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public void eliminar(Long id, String usernameActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        if (usuario.getUsername().equals(usernameActual)) {
            throw new ConflictException("No puede eliminar su propio usuario");
        }
        // Desactivación lógica: el usuario puede tener ventas asociadas
        usuario.setActivo(false);
    }
}
