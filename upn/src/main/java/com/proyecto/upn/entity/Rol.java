package com.proyecto.upn.entity;

/**
 * Rol del usuario del sistema. Se guarda como texto (EnumType.STRING) en una columna VARCHAR(20), no como ordinal:
 * agregar un rol nuevo no cambia el significado de los datos que ya existen.
 */
public enum Rol {
    /** Administra inventario y usuarios, edita o anula préstamos y aprueba o rechaza solicitudes. */
    ADMIN,
    /** Encargado de préstamos: registra solicitantes, préstamos y devoluciones. */
    ENCARGADO,
    /** Estudiante vinculado a un Solicitante: ve el catálogo y solicita préstamos que el ADMIN aprueba. */
    ALUMNO
}
