package com.proyecto.upn.exception;

/** Se lanza ante datos duplicados o que entran en conflicto con otros registros (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}
