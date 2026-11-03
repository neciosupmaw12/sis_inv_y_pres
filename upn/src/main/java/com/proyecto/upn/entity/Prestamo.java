package com.proyecto.upn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestamos")
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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPrestamo estado = EstadoPrestamo.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id")
    private Solicitante solicitante;

    /** Usuario del sistema que registró el préstamo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario registradoPor;

    @Column(length = 255)
    private String observaciones;

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
