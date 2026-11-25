package com.proyecto.upn;

import com.proyecto.upn.dto.DetallePrestamoRequest;
import com.proyecto.upn.dto.EquipoResponse;
import com.proyecto.upn.dto.PageResponse;
import com.proyecto.upn.dto.PrestamoRequest;
import com.proyecto.upn.dto.PrestamoResponse;
import com.proyecto.upn.entity.*;
import com.proyecto.upn.exception.BusinessException;
import com.proyecto.upn.exception.StockInsuficienteException;
import com.proyecto.upn.repository.EquipoRepository;
import com.proyecto.upn.repository.PrestamoRepository;
import com.proyecto.upn.repository.SolicitanteRepository;
import com.proyecto.upn.service.EquipoService;
import com.proyecto.upn.service.PrestamoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de transacciones y paginación contra MySQL (base upn_inventario_test, perfil "test").
 * <p>
 * Los métodos de prueba NO llevan @Transactional a propósito: si la prueba abriera su propia transacción, el rollback
 * del servicio quedaría "dentro" de ella y no se podría comprobar. Así, cada llamada al servicio confirma (COMMIT) o
 * revierte (ROLLBACK) de verdad, y después se vuelve a consultar la base para verificar el estado real.
 */
@SpringBootTest
@ActiveProfiles("test")
class PrestamoTransaccionTest {

    private static final String PREFIJO = "TST-";
    private static final String USUARIO = "admin"; // lo crea DataInitializer también en la base de pruebas

    @Autowired private PrestamoService prestamoService;
    @Autowired private EquipoService equipoService;
    @Autowired private EquipoRepository equipoRepository;
    @Autowired private PrestamoRepository prestamoRepository;
    @Autowired private SolicitanteRepository solicitanteRepository;

    private Solicitante solicitante;

    @BeforeEach
    void crearSolicitante() {
        solicitante = solicitanteRepository.save(Solicitante.builder()
                .dni("99" + String.format("%06d", (int) (Math.random() * 1_000_000)))
                .nombres("Prueba").apellidos("Transacciones").tipo(TipoSolicitante.ESTUDIANTE).build());
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        prestamoRepository.deleteAll(prestamoRepository.findAll().stream()
                .filter(p -> p.getSolicitante().getId().equals(solicitante.getId())).toList());
        equipoRepository.deleteAll(equipoRepository.findAll().stream()
                .filter(e -> e.getCodigo().startsWith(PREFIJO)).toList());
        solicitanteRepository.deleteById(solicitante.getId());
    }

    @Test
    @DisplayName("1. Transacción exitosa: préstamo de 2 equipos se guarda y descuenta el stock")
    void prestamoExitosoDescuentaStock() {
        Equipo laptop = equipo("TST-LAP", 5, EstadoEquipo.OPERATIVO);
        Equipo proyector = equipo("TST-PRY", 3, EstadoEquipo.OPERATIVO);

        PrestamoResponse creado = prestamoService.crear(
                solicitud(linea(laptop, 2), linea(proyector, 1)), USUARIO);

        assertThat(creado.id()).isNotNull();
        assertThat(creado.estado()).isEqualTo("ACTIVO");
        assertThat(creado.totalEquipos()).isEqualTo(3);
        assertThat(disponibles(laptop)).isEqualTo(3);
        assertThat(disponibles(proyector)).isEqualTo(2);
        assertThat(prestamoRepository.findWithDetallesById(creado.id()))
                .hasValueSatisfying(p -> assertThat(p.getDetalles()).hasSize(2));
    }

    @Test
    @DisplayName("2. Rollback por stock: si el 3.er equipo no alcanza, ningún stock cambia y no hay préstamo")
    void rollbackPorStockInsuficiente() {
        Equipo laptop = equipo("TST-LAP", 5, EstadoEquipo.OPERATIVO);
        Equipo tablet = equipo("TST-TAB", 4, EstadoEquipo.OPERATIVO);
        Equipo camara = equipo("TST-CAM", 1, EstadoEquipo.OPERATIVO);
        long prestamosAntes = prestamoRepository.count();

        // Los dos primeros equipos sí se descuentan en memoria antes de que falle el tercero
        assertThatThrownBy(() -> prestamoService.crear(
                solicitud(linea(laptop, 2), linea(tablet, 1), linea(camara, 3)), USUARIO))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Disponibles: 1, solicitadas: 3");

        // Se consulta otra vez la base: el ROLLBACK revirtió todo
        assertThat(disponibles(laptop)).isEqualTo(5);
        assertThat(disponibles(tablet)).isEqualTo(4);
        assertThat(disponibles(camara)).isEqualTo(1);
        assertThat(prestamoRepository.count()).isEqualTo(prestamosAntes);
        assertThat(prestamosDelSolicitante()).isEmpty();
    }

    @Test
    @DisplayName("3. Rollback por equipo no operativo: ningún stock cambia y no hay préstamo")
    void rollbackPorEquipoNoOperativo() {
        Equipo laptop = equipo("TST-LAP", 5, EstadoEquipo.OPERATIVO);
        Equipo enReparacion = equipo("TST-MNT", 4, EstadoEquipo.EN_MANTENIMIENTO);

        assertThatThrownBy(() -> prestamoService.crear(
                solicitud(linea(laptop, 2), linea(enReparacion, 1)), USUARIO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está operativo");

        assertThat(disponibles(laptop)).isEqualTo(5);
        assertThat(disponibles(enReparacion)).isEqualTo(4);
        assertThat(prestamosDelSolicitante()).isEmpty();
    }

    @Test
    @DisplayName("4. Devolución exitosa: el stock de todos los equipos se repone")
    void devolucionReponeStock() {
        Equipo laptop = equipo("TST-LAP", 5, EstadoEquipo.OPERATIVO);
        Equipo proyector = equipo("TST-PRY", 3, EstadoEquipo.OPERATIVO);
        PrestamoResponse creado = prestamoService.crear(
                solicitud(linea(laptop, 4), linea(proyector, 3)), USUARIO);
        assertThat(disponibles(laptop)).isEqualTo(1);
        assertThat(disponibles(proyector)).isZero();

        PrestamoResponse devuelto = prestamoService.devolver(creado.id());

        assertThat(devuelto.estado()).isEqualTo("DEVUELTO");
        assertThat(devuelto.fechaDevolucionReal()).isNotNull();
        assertThat(disponibles(laptop)).isEqualTo(5);
        assertThat(disponibles(proyector)).isEqualTo(3);
    }

    @Test
    @DisplayName("5. Paginación: totalElements, totalPages y tamaño de página")
    void paginacionDeEquipos() {
        for (int i = 1; i <= 12; i++) {
            equipo(String.format("TST-PAG-%02d", i), 2, EstadoEquipo.OPERATIVO);
        }

        PageResponse<EquipoResponse> primera = equipoService.listarPaginado(0, 5, "codigo,asc", "tst-pag", null, null);
        assertThat(primera.totalElements()).isEqualTo(12);
        assertThat(primera.totalPages()).isEqualTo(3);
        assertThat(primera.size()).isEqualTo(5);
        assertThat(primera.content()).hasSize(5);
        assertThat(primera.first()).isTrue();
        assertThat(primera.last()).isFalse();
        assertThat(primera.content().getFirst().codigo()).isEqualTo("TST-PAG-01");

        PageResponse<EquipoResponse> ultima = equipoService.listarPaginado(2, 5, "codigo,asc", "tst-pag", null, null);
        assertThat(ultima.content()).hasSize(2);
        assertThat(ultima.last()).isTrue();
        assertThat(ultima.content().getLast().codigo()).isEqualTo("TST-PAG-12");

        // Validaciones de los parámetros
        assertThatThrownBy(() -> equipoService.listarPaginado(0, 51, null, null, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> equipoService.listarPaginado(0, 5, "version,asc", null, null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Campos permitidos");
    }

    @Test
    @DisplayName("6. Bloqueo optimista: dos copias del mismo equipo, la segunda escritura falla")
    void bloqueoOptimisticoConVersion() {
        Equipo original = equipo("TST-VER", 5, EstadoEquipo.OPERATIVO);
        Equipo copiaA = equipoRepository.findById(original.getId()).orElseThrow();
        Equipo copiaB = equipoRepository.findById(original.getId()).orElseThrow();

        copiaA.setCantidadDisponible(4);
        equipoRepository.save(copiaA);       // version 0 → 1

        copiaB.setCantidadDisponible(3);     // todavía cree que la versión es 0
        assertThatThrownBy(() -> equipoRepository.save(copiaB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(disponibles(original)).isEqualTo(4);
    }

    // ---------- utilidades ----------

    private Equipo equipo(String codigo, int cantidad, EstadoEquipo estado) {
        return equipoRepository.save(Equipo.builder().codigo(codigo).nombre("Equipo " + codigo).categoria("Pruebas")
                .cantidadTotal(cantidad).cantidadDisponible(cantidad).estado(estado).build());
    }

    private int disponibles(Equipo equipo) {
        return equipoRepository.findById(equipo.getId()).orElseThrow().getCantidadDisponible();
    }

    private List<Prestamo> prestamosDelSolicitante() {
        return prestamoRepository.findAll().stream()
                .filter(p -> p.getSolicitante().getId().equals(solicitante.getId())).toList();
    }

    private DetallePrestamoRequest linea(Equipo equipo, int cantidad) {
        return new DetallePrestamoRequest(equipo.getId(), cantidad);
    }

    private PrestamoRequest solicitud(DetallePrestamoRequest... lineas) {
        return new PrestamoRequest(solicitante.getId(), LocalDate.now().plusDays(7), "Prueba JUnit", List.of(lineas));
    }
}
