package com.proyecto.upn.exception;

/** Se lanza cuando una operación viola una regla de negocio, p. ej. stock insuficiente (HTTP 400). */
public class BusinessException extends RuntimeException {

    public BusinessException(String mensaje) {
        super(mensaje);
    }
}
