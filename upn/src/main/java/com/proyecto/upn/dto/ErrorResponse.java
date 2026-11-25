package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Cuerpo uniforme de error que devuelve GlobalExceptionHandler")
public record ErrorResponse(
        @Schema(example = "2026-11-24T10:15:02") LocalDateTime timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "No hay unidades suficientes de 'Proyector multimedia Epson' (PRY-001). Disponibles: 1, solicitadas: 3. No se registró el préstamo.") String mensaje,
        @Schema(example = "/api/prestamos") String ruta,
        @Schema(description = "Errores por campo (400) o detalle del conflicto (409)") Map<String, String> errores
) {
    public static ErrorResponse of(int status, String error, String mensaje, String ruta) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, ruta, null);
    }
}
