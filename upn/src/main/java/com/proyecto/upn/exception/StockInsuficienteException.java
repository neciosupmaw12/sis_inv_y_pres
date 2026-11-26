package com.proyecto.upn.exception;

import lombok.Getter;

/**
 * Se lanza cuando un préstamo pide más unidades de las disponibles (HTTP 409).
 * Es una RuntimeException: al salir de un método @Transactional provoca el ROLLBACK de toda la operación,
 * así que no queda descontado el stock de los equipos que ya se habían procesado.
 */
@Getter
public class StockInsuficienteException extends RuntimeException {

    private final Long equipoId;
    private final String codigo;
    private final int disponibles;
    private final int solicitadas;

    public StockInsuficienteException(Long equipoId, String codigo, String nombre, int disponibles, int solicitadas) {
        this(equipoId, codigo, nombre, disponibles, solicitadas, "No se registró el préstamo.");
    }

    /** @param resultado cierre del mensaje, por ejemplo "No se aprobó la solicitud." */
    public StockInsuficienteException(Long equipoId, String codigo, String nombre, int disponibles, int solicitadas,
                                      String resultado) {
        super("No hay unidades suficientes de '" + nombre + "' (" + codigo + "). Disponibles: " + disponibles
                + ", solicitadas: " + solicitadas + ". " + resultado);
        this.equipoId = equipoId;
        this.codigo = codigo;
        this.disponibles = disponibles;
        this.solicitadas = solicitadas;
    }
}
