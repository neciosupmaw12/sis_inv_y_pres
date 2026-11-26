package com.proyecto.upn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * VARCHAR(20) y no el ENUM nativo de MySQL: con ENUM('ADMIN','ENCARGADO') cada rol nuevo exigiría un ALTER TABLE
     * (ddl-auto=update no modifica columnas existentes). Ver {@code MigracionEsquema}.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}
