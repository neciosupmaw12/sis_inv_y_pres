package com.proyecto.upn.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Validación que depende de otro campo y no se puede expresar con una anotación (por ejemplo, "los datos del
 * estudiante son obligatorios si el rol es ALUMNO"). Responde 400 con el mismo formato que @Valid: detalle en
 * {@code errores}.
 */
@Getter
public class ValidacionException extends RuntimeException {

    private final Map<String, String> errores;

    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.errores = Map.of(campo, mensaje);
    }
}
