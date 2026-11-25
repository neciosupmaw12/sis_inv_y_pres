package com.proyecto.upn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "equipos", indexes = {
        // Filtros y orden de la búsqueda paginada (codigo ya tiene índice UNIQUE)
        @Index(name = "idx_equipos_nombre", columnList = "nombre"),
        @Index(name = "idx_equipos_estado_categoria", columnList = "estado, categoria"),
        @Index(name = "idx_equipos_categoria", columnList = "categoria"),
        @Index(name = "idx_equipos_marca", columnList = "marca")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(length = 50)
    private String marca;

    @Column(length = 50)
    private String modelo;

    @Column(length = 255)
    private String descripcion;

    /** Unidades que posee la institución. */
    @Column(nullable = false)
    private Integer cantidadTotal;

    /** Unidades en almacén (total menos las prestadas). */
    @Column(nullable = false)
    private Integer cantidadDisponible;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEquipo estado = EstadoEquipo.OPERATIVO;

    /**
     * Bloqueo optimista: Hibernate agrega "WHERE version = ?" al UPDATE e incrementa el valor. Si dos préstamos
     * descuentan stock del mismo equipo al mismo tiempo, el segundo UPDATE no encuentra la versión que leyó, falla con
     * OptimisticLockException y su transacción completa hace rollback (respuesta 409).
     * DEFAULT 0 para que las filas que ya existían reciban un valor al agregar la columna.
     */
    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    @JsonIgnore
    private Long version;
}
