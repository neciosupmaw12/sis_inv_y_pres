package com.proyecto.upn.entity;

/** Estado guardado del préstamo. "VENCIDO" no se guarda: se calcula cuando un préstamo ACTIVO pasó su fecha prevista. */
public enum EstadoPrestamo {
    ACTIVO,
    DEVUELTO
}
