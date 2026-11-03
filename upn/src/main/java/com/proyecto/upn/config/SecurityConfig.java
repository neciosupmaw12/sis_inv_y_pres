package com.proyecto.upn.config;

import com.proyecto.upn.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Gestión de usuarios: solo administrador
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        // Equipos (inventario): lectura para ambos roles, escritura solo administrador
                        .requestMatchers(HttpMethod.GET, "/api/equipos/**").hasAnyRole("ADMIN", "ENCARGADO")
                        .requestMatchers("/api/equipos/**").hasRole("ADMIN")
                        // Solicitantes: el encargado registra y edita; solo el administrador elimina
                        .requestMatchers(HttpMethod.DELETE, "/api/solicitantes/**").hasRole("ADMIN")
                        .requestMatchers("/api/solicitantes/**").hasAnyRole("ADMIN", "ENCARGADO")
                        // Préstamos: el encargado consulta, registra y recibe devoluciones;
                        // editar y anular solo administrador
                        .requestMatchers(HttpMethod.PUT, "/api/prestamos/*/devolucion").hasAnyRole("ADMIN", "ENCARGADO")
                        .requestMatchers(HttpMethod.PUT, "/api/prestamos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/prestamos/**").hasRole("ADMIN")
                        .requestMatchers("/api/prestamos/**").hasAnyRole("ADMIN", "ENCARGADO")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            Object detalle = req.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR);
                            escribirError(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                                    detalle != null ? detalle.toString() : "Debe iniciar sesión para acceder a este recurso");
                        })
                        .accessDeniedHandler((req, res, e) -> escribirError(req, res,
                                HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                                "No tiene permisos para realizar esta operación")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** El filtro JWT solo debe ejecutarse dentro de la cadena de Spring Security, no como filtro global. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private static void escribirError(HttpServletRequest req, HttpServletResponse res, int status,
                                      String error, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("""
                {"timestamp":"%s","status":%d,"error":"%s","mensaje":"%s","ruta":"%s"}"""
                .formatted(LocalDateTime.now(), status, error, mensaje, req.getRequestURI()));
    }
}
