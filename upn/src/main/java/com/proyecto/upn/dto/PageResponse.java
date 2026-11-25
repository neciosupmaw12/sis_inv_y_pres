package com.proyecto.upn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados con un formato JSON estable.
 * <p>
 * No se serializa {@code PageImpl} directamente: Spring Data advierte que su JSON no es estable entre versiones
 * (expone detalles internos como {@code pageable} y {@code sort}). Este record define un contrato propio, simple y
 * documentado en Swagger, que coincide con la interfaz {@code Pagina<T>} de Angular.
 */
@Schema(description = "Página de resultados")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página actual") List<T> content,
        @Schema(description = "Número de página (empieza en 0)", example = "0") int page,
        @Schema(description = "Tamaño de página solicitado", example = "10") int size,
        @Schema(description = "Total de registros que cumplen el filtro", example = "87") long totalElements,
        @Schema(description = "Total de páginas", example = "9") int totalPages,
        @Schema(description = "¿Es la primera página?", example = "true") boolean first,
        @Schema(description = "¿Es la última página?", example = "false") boolean last,
        @Schema(description = "Orden aplicado", example = "nombre,asc") String sort
) {
    public static <E, T> PageResponse<T> of(Page<E> pagina, Function<E, T> mapper, String sort) {
        return of(pagina, pagina.getContent().stream().map(mapper).toList(), sort);
    }

    /** Usa un contenido ya convertido (por ejemplo, cuando se cargó con una segunda consulta). */
    public static <T> PageResponse<T> of(Page<?> pagina, List<T> contenido, String sort) {
        return new PageResponse<>(contenido, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages(), pagina.isFirst(), pagina.isLast(), sort);
    }
}
