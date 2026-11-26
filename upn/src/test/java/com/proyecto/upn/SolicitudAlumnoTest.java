package com.proyecto.upn;

import com.proyecto.upn.entity.*;
import com.proyecto.upn.repository.EquipoRepository;
import com.proyecto.upn.repository.PrestamoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.repository.UsuarioRepository;
import com.proyecto.upn.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rol ALUMNO y flujo de aprobación, de punta a punta: peticiones HTTP reales (MockMvc) con tokens JWT generados por
 * {@link JwtService}, contra MySQL (base upn_inventario_test, perfil "test").
 * <p>
 * Igual que {@code PrestamoTransaccionTest}, los métodos NO llevan @Transactional: cada petición confirma o revierte
 * su propia transacción y después se vuelve a consultar la base para ver el estado real (stock y estado).
 * El usuario alumno / alumno123 lo crea DataInitializer también en la base de pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitudAlumnoTest {

    private static final String PREFIJO = "TSA-";
    private static final String MANANA = LocalDate.now().plusDays(3).toString();

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SolicitanteRepository solicitanteRepository;
    @Autowired private EquipoRepository equipoRepository;
    @Autowired private PrestamoRepository prestamoRepository;

    private String tokenAdmin;
    private String tokenAlumno;
    private Long solicitanteAlumno;

    @BeforeEach
    void tokens() {
        Usuario alumno = usuarioRepository.findWithSolicitanteByUsername("alumno").orElseThrow();
        solicitanteAlumno = alumno.getSolicitante().getId();
        tokenAlumno = jwtService.generarToken(alumno);
        tokenAdmin = jwtService.generarToken(usuarioRepository.findWithSolicitanteByUsername("admin").orElseThrow());
    }

    @AfterEach
    void limpiar() {
        // findAllByOrderByFechaPrestamoDesc trae detalles y equipos (no hay transacción abierta para cargarlos después)
        prestamoRepository.deleteAll(prestamoRepository.findAllByOrderByFechaPrestamoDesc().stream()
                .filter(p -> p.getDetalles().stream().anyMatch(d -> d.getEquipo().getCodigo().startsWith(PREFIJO))
                        || p.getSolicitante().getId().equals(solicitanteAlumno))
                .toList());
        equipoRepository.deleteAll(equipoRepository.findAll().stream()
                .filter(e -> e.getCodigo().startsWith(PREFIJO)).toList());
        usuarioRepository.findByUsername("tsa.alumno").ifPresent(usuarioRepository::delete);
        usuarioRepository.findByUsername("tsa.otro").ifPresent(usuarioRepository::delete);
        solicitanteRepository.findByDni("79000001").ifPresent(solicitanteRepository::delete);
        solicitanteRepository.findByDni("79000002").ifPresent(solicitanteRepository::delete);
    }

    @Test
    @DisplayName("1. El alumno crea una solicitud: queda PENDIENTE y el stock no cambia")
    void solicitudQuedaPendienteSinTocarStock() throws Exception {
        Equipo laptop = equipo("TSA-LAP", 5);

        Long id = solicitar(linea(laptop, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.solicitanteId").value(solicitanteAlumno))
                .andReturnIdFrom();

        assertThat(disponibles(laptop)).isEqualTo(5);
        assertThat(estado(id)).isEqualTo(EstadoPrestamo.PENDIENTE);
    }

    @Test
    @DisplayName("2. El admin aprueba: pasa a ACTIVO, baja el stock y se guarda quién y cuándo")
    void aprobarDescuentaStock() throws Exception {
        Equipo laptop = equipo("TSA-LAP", 5);
        Long id = solicitar(linea(laptop, 2)).andReturnIdFrom();

        perform(put("/api/prestamos/" + id + "/aprobar"), tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.aprobadoPor").value("Administrador"));

        assertThat(disponibles(laptop)).isEqualTo(3);
        Prestamo p = prestamoRepository.findWithDetallesById(id).orElseThrow();
        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.ACTIVO);
        assertThat(p.getAprobadoPor().getUsername()).isEqualTo("admin");
        assertThat(p.getFechaAprobacion()).isNotNull();
    }

    @Test
    @DisplayName("3. Aprobación sin stock suficiente: 409 y ROLLBACK (stock y estado sin cambios)")
    void aprobarSinStockHaceRollback() throws Exception {
        Equipo laptop = equipo("TSA-LAP", 5);
        Equipo proyector = equipo("TSA-PRY", 3);
        Long id = solicitar(linea(laptop, 2), linea(proyector, 3)).andReturnIdFrom();

        // Mientras la solicitud espera, el proyector se queda con 1 unidad (por ejemplo, por un préstamo directo)
        proyector = equipoRepository.findById(proyector.getId()).orElseThrow();
        proyector.setCantidadDisponible(1);
        equipoRepository.save(proyector);

        // La laptop sí alcanza y se descuenta en memoria antes de que falle el proyector
        perform(put("/api/prestamos/" + id + "/aprobar"), tokenAdmin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.codigo").value("TSA-PRY"))
                .andExpect(jsonPath("$.errores.disponibles").value("1"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("No se aprobó la solicitud")));

        assertThat(disponibles(laptop)).isEqualTo(5);           // el ROLLBACK revirtió el descuento de la laptop
        assertThat(disponibles(proyector)).isEqualTo(1);
        assertThat(estado(id)).isEqualTo(EstadoPrestamo.PENDIENTE);
    }

    @Test
    @DisplayName("4. Rechazo: RECHAZADO, stock igual y motivo guardado (el motivo es obligatorio)")
    void rechazarGuardaMotivo() throws Exception {
        Equipo laptop = equipo("TSA-LAP", 5);
        Long id = solicitar(linea(laptop, 1)).andReturnIdFrom();

        perform(put("/api/prestamos/" + id + "/rechazar").content("{\"motivo\":\"\"}"), tokenAdmin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
        perform(put("/api/prestamos/" + id + "/rechazar").content("{\"motivo\":\"No hay laptops esa semana\"}"), tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));

        assertThat(disponibles(laptop)).isEqualTo(5);
        Prestamo p = prestamoRepository.findWithDetallesById(id).orElseThrow();
        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.RECHAZADO);
        assertThat(p.getMotivoRechazo()).isEqualTo("No hay laptops esa semana");
        assertThat(p.getAprobadoPor().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("5. El alumno no puede aprobar, ver préstamos ajenos ni entrar a /api/usuarios (403)")
    void alumnoSinPermisos() throws Exception {
        Equipo laptop = equipo("TSA-LAP", 5);
        Long propia = solicitar(linea(laptop, 1)).andReturnIdFrom();
        Long ajeno = prestamoDirectoDeOtro(laptop);

        perform(put("/api/prestamos/" + propia + "/aprobar"), tokenAlumno).andExpect(status().isForbidden());
        perform(get("/api/prestamos/" + ajeno), tokenAlumno).andExpect(status().isForbidden());
        perform(put("/api/prestamos/" + ajeno + "/cancelar"), tokenAlumno).andExpect(status().isForbidden());
        perform(get("/api/prestamos/paginado"), tokenAlumno).andExpect(status().isForbidden());
        perform(get("/api/usuarios"), tokenAlumno).andExpect(status().isForbidden());
        perform(get("/api/panel/resumen"), tokenAlumno).andExpect(status().isForbidden());

        // Lo suyo sí lo ve, y "mis préstamos" no incluye el ajeno
        perform(get("/api/prestamos/" + propia), tokenAlumno).andExpect(status().isOk());
        perform(get("/api/prestamos/mis-prestamos?size=50"), tokenAlumno)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(ajeno.intValue()))));
        assertThat(estado(propia)).isEqualTo(EstadoPrestamo.PENDIENTE);
    }

    @Test
    @DisplayName("6. Crear un alumno con un DNI ya vinculado: 409, sin usuario ni solicitante nuevos")
    void alumnoConDniDuplicado() throws Exception {
        perform(post("/api/usuarios").content(altaAlumno("tsa.alumno", "79000001", "tsa1@upn.pe")), tokenAdmin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("79000001"));
        long solicitantes = solicitanteRepository.count();
        long usuarios = usuarioRepository.count();

        perform(post("/api/usuarios").content(altaAlumno("tsa.otro", "79000001", "tsa2@upn.pe")), tokenAdmin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El estudiante con DNI 79000001 ya tiene el usuario 'tsa.alumno'"));

        assertThat(usuarioRepository.findByUsername("tsa.otro")).isEmpty();
        assertThat(solicitanteRepository.count()).isEqualTo(solicitantes);
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
    }

    @Test
    @DisplayName("6b. DNI nuevo pero usuario duplicado: 409 y el ROLLBACK borra también el estudiante insertado")
    void altaAlumnoEsUnaSolaTransaccion() throws Exception {
        // El estudiante se inserta primero (paso 1) y el usuario duplicado falla después (paso 2)
        perform(post("/api/usuarios").content(altaAlumno("admin", "79000002", "tsa3@upn.pe")), tokenAdmin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El usuario admin ya existe"));

        assertThat(solicitanteRepository.findByDni("79000002")).isEmpty();
    }

    // ------------------------------------------------------------------ utilidades

    private Equipo equipo(String codigo, int cantidad) {
        return equipoRepository.save(Equipo.builder().codigo(codigo).nombre("Equipo " + codigo).categoria("Pruebas")
                .cantidadTotal(cantidad).cantidadDisponible(cantidad).estado(EstadoEquipo.OPERATIVO).build());
    }

    private String linea(Equipo equipo, int cantidad) {
        return "{\"equipoId\":" + equipo.getId() + ",\"cantidad\":" + cantidad + "}";
    }

    private Respuesta solicitar(String... lineas) throws Exception {
        String body = "{\"fechaDevolucionPrevista\":\"" + MANANA + "\",\"motivo\":\"Prueba de solicitud\","
                + "\"detalles\":[" + String.join(",", lineas) + "]}";
        return new Respuesta(perform(post("/api/prestamos/solicitudes").content(body), tokenAlumno));
    }

    /** Préstamo de otro solicitante (el docente de DataInitializer), registrado por el admin. */
    private Long prestamoDirectoDeOtro(Equipo equipo) throws Exception {
        Long otro = solicitanteRepository.findByDni("45678912").orElseThrow().getId();
        String body = "{\"solicitanteId\":" + otro + ",\"fechaDevolucionPrevista\":\"" + MANANA + "\","
                + "\"detalles\":[" + linea(equipo, 1) + "]}";
        return new Respuesta(perform(post("/api/prestamos").content(body), tokenAdmin)
                .andExpect(status().isCreated())).andReturnIdFrom();
    }

    private String altaAlumno(String username, String dni, String email) {
        return """
                {"username":"%s","password":"clave123","rol":"ALUMNO",
                 "alumno":{"dni":"%s","nombres":"Prueba","apellidos":"Alumno Test","email":"%s","area":"Sistemas"}}"""
                .formatted(username, dni, email);
    }

    private ResultActions perform(MockHttpServletRequestBuilder peticion, String token) throws Exception {
        return mvc.perform(peticion.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON));
    }

    private int disponibles(Equipo equipo) {
        return equipoRepository.findById(equipo.getId()).orElseThrow().getCantidadDisponible();
    }

    private EstadoPrestamo estado(Long prestamoId) {
        return prestamoRepository.findById(prestamoId).orElseThrow().getEstado();
    }

    /** Envoltorio para leer el id del préstamo creado de la respuesta JSON. */
    private record Respuesta(ResultActions acciones) {
        Respuesta andExpect(org.springframework.test.web.servlet.ResultMatcher m) throws Exception {
            acciones.andExpect(m);
            return this;
        }

        Long andReturnIdFrom() throws Exception {
            String json = acciones.andReturn().getResponse().getContentAsString();
            return Long.valueOf(json.replaceAll("(?s)^\\{\"id\":(\\d+).*$", "$1"));
        }
    }
}
