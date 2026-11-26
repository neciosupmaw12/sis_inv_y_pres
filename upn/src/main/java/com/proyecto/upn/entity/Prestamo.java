package com.proyecto.upn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestamos", indexes = {
        // Filtro por estado + vencimiento (activos, vencidos, devueltos) y orden por fecha
        @Index(name = "idx_prestamos_estado_fecha_prevista", columnList = "estado, fechaDevolucionPrevista"),
        @Index(name = "idx_prestamos_fecha_prestamo", columnList = "fechaPrestamo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaPrestamo;

    @Column(nullable = false)
    private LocalDate fechaDevolucionPrevista;

    private LocalDateTime fechaDevolucionReal;

    /** VARCHAR(20) en lugar del ENUM nativo de MySQL, igual que {@link Usuario#getRol()}. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPrestamo estado = EstadoPrestamo.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id")
    private Solicitante solicitante;

    /** Usuario del sistema que registró el préstamo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario registradoPor;

    /** Observaciones del préstamo directo, o el motivo que escribió el alumno en su solicitud. */
    @Column(length = 255)
    private String observaciones;

    /** Cuándo el alumno envió la solicitud; null en los préstamos directos de ADMIN / ENCARGADO. */
    private LocalDateTime fechaSolicitud;

    /** ADMIN que aprobó o rechazó la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id")
    private Usuario aprobadoPor;

    /** Cuándo se aprobó o rechazó la solicitud. */
    private LocalDateTime fechaAprobacion;

    /** Obligatorio al rechazar; se muestra al alumno en "Mis préstamos". */
    @Column(length = 255)
    private String motivoRechazo;

    @Builder.Default
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePrestamo> detalles = new ArrayList<>();

    public void agregarDetalle(DetallePrestamo detalle) {
        detalle.setPrestamo(this);
        detalles.add(detalle);
    }

    public boolean isVencido() {
        return estado == EstadoPrestamo.ACTIVO && fechaDevolucionPrevista.isBefore(LocalDate.now());
    }
}
