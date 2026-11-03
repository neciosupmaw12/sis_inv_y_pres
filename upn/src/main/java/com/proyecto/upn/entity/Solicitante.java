package com.proyecto.upn.entity;

import jakarta.persistence.*;
import lombok.*;

/** Persona que solicita equipos en préstamo (estudiante, docente o administrativo). */
@Entity
@Table(name = "solicitantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSolicitante tipo;

    /** Carrera (estudiantes) o área de trabajo (docentes y administrativos). */
    @Column(length = 100)
    private String area;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 9)
    private String telefono;
}
