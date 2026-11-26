package com.proyecto.upn.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * Datos de la sesión leídos del JWT ya validado; {@link JwtAuthenticationFilter} los guarda en
 * {@code Authentication.getDetails()}.
 * <p>
 * El {@code solicitanteId} de un ALUMNO sale SIEMPRE de aquí (del token firmado por el servidor), nunca de un
 * parámetro o del cuerpo del request: así un alumno no puede pedir préstamos a nombre de otro ni ver los ajenos.
 */
public record SesionJwt(String username, String rol, Long solicitanteId) {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_SOLICITANTE = "solicitanteId";

    /** Id del solicitante del alumno autenticado; 403 si el token no lo tiene (ADMIN, ENCARGADO o token antiguo). */
    public static Long solicitanteId(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof SesionJwt s && s.solicitanteId() != null) {
            return s.solicitanteId();
        }
        throw new AccessDeniedException("El usuario no está vinculado a un solicitante");
    }

    public static boolean esAlumno(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ALUMNO".equals(a.getAuthority()));
    }
}
