package com.proyecto.upn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Migración de esquema idempotente que se ejecuta al arrancar, ANTES de {@link DataInitializer}.
 * <p>
 * <b>Por qué hace falta.</b> Los enums {@code Rol} y {@code EstadoPrestamo} ya se guardaban como texto
 * ({@code EnumType.STRING}, no ORDINAL), pero Hibernate creó las columnas con el tipo ENUM nativo de MySQL:
 * {@code usuarios.rol ENUM('ADMIN','ENCARGADO')} y {@code prestamos.estado ENUM('ACTIVO','DEVUELTO')}.
 * Con {@code ddl-auto=update} Hibernate agrega columnas nuevas, pero nunca modifica las existentes; así que al
 * guardar {@code ALUMNO} o {@code PENDIENTE} MySQL respondería "Data truncated for column".
 * <p>
 * <b>Qué hace.</b> Si la columna todavía es ENUM, la convierte a {@code VARCHAR(20)}. MySQL conserva el texto de cada
 * fila ('ADMIN' sigue siendo 'ADMIN'), y el índice {@code idx_prestamos_estado_fecha_prevista} se mantiene.
 * Si la columna ya es VARCHAR (bases nuevas, o la migración ya corrió), no hace nada.
 */
@Slf4j
@Component
@Order(1)
public class MigracionEsquema implements CommandLineRunner {

    /** Tabla, columna y definición final de cada columna que deja de ser ENUM. */
    private record Columna(String tabla, String columna, String definicion) {
    }

    private static final List<Columna> COLUMNAS = List.of(
            new Columna("usuarios", "rol", "VARCHAR(20) NOT NULL"),
            new Columna("prestamos", "estado", "VARCHAR(20) NOT NULL"));

    private final JdbcTemplate jdbc;

    public MigracionEsquema(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) {
        for (Columna c : COLUMNAS) {
            String tipo = jdbc.query("""
                            SELECT DATA_TYPE FROM information_schema.COLUMNS
                            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?""",
                    rs -> rs.next() ? rs.getString(1) : null, c.tabla(), c.columna());
            if ("enum".equalsIgnoreCase(tipo)) {
                // Nombres fijos de esta clase (no vienen del usuario): no hay riesgo de inyección SQL
                jdbc.execute("ALTER TABLE " + c.tabla() + " MODIFY " + c.columna() + " " + c.definicion());
                log.info("Migración: {}.{} cambió de ENUM a {}", c.tabla(), c.columna(), c.definicion());
            }
        }
    }
}
