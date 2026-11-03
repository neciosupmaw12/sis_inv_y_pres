package com.proyecto.upn.config;

import com.proyecto.upn.entity.*;
import com.proyecto.upn.repository.EquipoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Crea usuarios y datos de ejemplo la primera vez que se inicia la aplicación. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        usuario("admin", "admin123", "Administrador", Rol.ADMIN);
        usuario("encargado", "encargado123", "Encargado de Préstamos", Rol.ENCARGADO);
        // Integrantes del grupo 06
        usuario("lfernandez", "upn2026", "Leslie Fernández Toro", Rol.ADMIN);
        usuario("rquispe", "upn2026", "Rodolfo Quispe Quispe", Rol.ADMIN);
        usuario("mneciosup", "upn2026", "Marco Neciosup Panibra", Rol.ADMIN);
        usuario("ntroncos", "upn2026", "Nelson Raúl Troncos Pintado", Rol.ADMIN);
        usuario("jmorales", "upn2026", "Juan Carlos Morales Rojas", Rol.ADMIN);

        if (equipoRepository.count() == 0) {
            equipoRepository.saveAll(List.of(
                    equipo("LAP-001", "Laptop Lenovo ThinkPad E14", "Laptops", "Lenovo", "ThinkPad E14 Gen 5", 10),
                    equipo("LAP-002", "Laptop HP ProBook 450", "Laptops", "HP", "ProBook 450 G10", 6),
                    equipo("PRY-001", "Proyector multimedia Epson", "Proyectores", "Epson", "PowerLite X49", 5),
                    equipo("TAB-001", "Tablet Samsung Galaxy Tab", "Tablets", "Samsung", "Galaxy Tab A9+", 8),
                    equipo("CAM-001", "Cámara web Full HD", "Periféricos", "Logitech", "C920", 12),
                    equipo("AUD-001", "Kit de micrófono inalámbrico", "Audio", "Shure", "BLX24", 3)));
            log.info("Inventario inicial de equipos creado");
        }
        if (solicitanteRepository.count() == 0) {
            solicitanteRepository.saveAll(List.of(
                    Solicitante.builder().dni("70123456").nombres("María").apellidos("Quispe Torres")
                            .tipo(TipoSolicitante.ESTUDIANTE).area("Ingeniería de Sistemas")
                            .email("maria.quispe@upn.pe").telefono("987654321").build(),
                    Solicitante.builder().dni("45678912").nombres("Carlos").apellidos("Ramírez Soto")
                            .tipo(TipoSolicitante.DOCENTE).area("Facultad de Ingeniería")
                            .email("carlos.ramirez@upn.pe").telefono("912345678").build()));
        }
    }

    /** Crea el usuario solo si aún no existe. */
    private void usuario(String username, String password, String nombre, Rol rol) {
        if (!usuarioRepository.existsByUsername(username)) {
            usuarioRepository.save(Usuario.builder().username(username).password(passwordEncoder.encode(password))
                    .nombre(nombre).rol(rol).build());
            log.info("Usuario inicial creado: {} ({})", username, rol);
        }
    }

    private Equipo equipo(String codigo, String nombre, String categoria, String marca, String modelo, int cantidad) {
        return Equipo.builder().codigo(codigo).nombre(nombre).categoria(categoria).marca(marca).modelo(modelo)
                .cantidadTotal(cantidad).cantidadDisponible(cantidad).estado(EstadoEquipo.OPERATIVO).build();
    }
}
