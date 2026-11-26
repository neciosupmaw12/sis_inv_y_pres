package com.proyecto.upn.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Lee el header "Authorization: Bearer <token>" y autentica la petición si el JWT es válido. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_ERROR = "jwt_error";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.validar(token);
            UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
            if (user.isEnabled()) {
                // Los permisos (ROLE_*) se leen de la base en cada petición; el solicitanteId, del token firmado
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                Object solicitante = claims.get(SesionJwt.CLAIM_SOLICITANTE);
                auth.setDetails(new SesionJwt(claims.getSubject(), claims.get(SesionJwt.CLAIM_ROL, String.class),
                        solicitante instanceof Number n ? n.longValue() : null));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                request.setAttribute(ATRIBUTO_ERROR, "El usuario está desactivado");
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute(ATRIBUTO_ERROR, "El token ha expirado");
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            request.setAttribute(ATRIBUTO_ERROR, "Token inválido");
        }
        chain.doFilter(request, response);
    }
}
