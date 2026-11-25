package com.proyecto.upn.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI 3 de la API REST.
 * Swagger UI: http://localhost:8080/swagger-ui.html · JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    public static final String SEGURIDAD_JWT = "bearerAuth";

    @Bean
    public OpenAPI apiInventario() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Inventario y Préstamo de Equipos UPN")
                        .version("1.1.0")
                        .description("""
                                API REST del sistema de inventario y préstamo de equipo tecnológico (Grupo 06).

                                **Cómo probar los endpoints protegidos:**
                                1. Ejecute `POST /api/auth/login` con `{"username":"admin","password":"admin123"}`.
                                2. Copie el valor de `token` de la respuesta.
                                3. Pulse **Authorize**, pegue el token (sin la palabra *Bearer*) y confirme.

                                Los listados paginados usan `page` (desde 0), `size` (1 a 50) y `sort` (`campo,asc|desc`).""")
                        .contact(new Contact().name("Grupo 06 - Universidad Privada del Norte")))
                .components(new Components().addSecuritySchemes(SEGURIDAD_JWT, new SecurityScheme()
                        .name(SEGURIDAD_JWT)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtenido en POST /api/auth/login")))
                // Todas las operaciones usan el token salvo las marcadas con @SecurityRequirements() vacío (login)
                .addSecurityItem(new SecurityRequirement().addList(SEGURIDAD_JWT));
    }
}
