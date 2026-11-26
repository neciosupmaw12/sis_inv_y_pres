package com.proyecto.upn.entity;

/**
 * Estado guardado del préstamo. "VENCIDO" no se guarda: se calcula cuando un préstamo ACTIVO pasó su fecha prevista.
 * <pre>
 * Solicitud del alumno:  PENDIENTE ──aprobar (ADMIN)──▶ ACTIVO ──devolver──▶ DEVUELTO
 *                          ├──rechazar (ADMIN)──▶ RECHAZADO
 *                          └──cancelar (alumno)──▶ CANCELADO
 * Préstamo directo de ADMIN / ENCARGADO: nace ACTIVO.
 * </pre>
 * Solo ACTIVO descuenta stock; PENDIENTE, RECHAZADO y CANCELADO nunca lo tocan.
 */
public enum EstadoPrestamo {
    PENDIENTE,
    ACTIVO,
    DEVUELTO,
    RECHAZADO,
    CANCELADO
}
