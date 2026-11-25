package com.proyecto.upn.util;

import com.proyecto.upn.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Convierte los parámetros {@code page}, {@code size} y {@code sort} de la URL en un {@link Pageable} seguro.
 * <ul>
 *   <li>{@code page} &ge; 0 y {@code size} entre 1 y {@value #TAMANO_MAXIMO}.</li>
 *   <li>{@code sort} con formato {@code campo,asc|desc}; solo se aceptan los campos de la lista blanca, para que
 *       no se pueda ordenar por columnas internas ni provocar errores de consulta.</li>
 *   <li>Se agrega {@code id} como desempate para que el orden sea estable entre páginas.</li>
 * </ul>
 */
public final class Paginacion {

    public static final int TAMANO_POR_DEFECTO = 10;
    public static final int TAMANO_MAXIMO = 50;

    private Paginacion() {
    }

    /**
     * @param camposPermitidos campo de la API → ruta de la propiedad JPA (por ejemplo, "solicitante" → "solicitante.apellidos")
     * @param ordenPorDefecto  valor de {@code sort} si no se envía, por ejemplo "nombre,asc"
     */
    public static Pageable crear(Integer page, Integer size, String sort, Map<String, String> camposPermitidos,
                                 String ordenPorDefecto) {
        int pagina = page == null ? 0 : page;
        int tamano = size == null ? TAMANO_POR_DEFECTO : size;
        if (pagina < 0) {
            throw new BusinessException("El parámetro 'page' debe ser mayor o igual a 0");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new BusinessException("El parámetro 'size' debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        return PageRequest.of(pagina, tamano, orden(normalizarOrden(sort, ordenPorDefecto), camposPermitidos));
    }

    /** Devuelve el orden en formato "campo,direccion" (el que se envía de vuelta al cliente). */
    public static String normalizarOrden(String sort, String ordenPorDefecto) {
        String valor = sort == null || sort.isBlank() ? ordenPorDefecto : sort.trim();
        String[] partes = valor.split(",");
        String direccion = partes.length > 1 ? partes[1].trim().toLowerCase() : "asc";
        return partes[0].trim() + "," + direccion;
    }

    private static Sort orden(String sort, Map<String, String> camposPermitidos) {
        String[] partes = sort.split(",");
        String campo = partes[0];
        String propiedad = camposPermitidos.get(campo);
        if (propiedad == null) {
            throw new BusinessException("No se puede ordenar por '" + campo + "'. Campos permitidos: "
                    + String.join(", ", camposPermitidos.keySet()));
        }
        Sort.Direction direccion = switch (partes[1]) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BusinessException("La dirección de orden debe ser 'asc' o 'desc'");
        };
        Sort resultado = Sort.by(direccion, propiedad);
        return "id".equals(propiedad) ? resultado : resultado.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    /** Convierte un texto de búsqueda en un patrón LIKE en minúsculas, o null si está vacío. */
    public static String patron(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return null;
        }
        String limpio = busqueda.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }
}
