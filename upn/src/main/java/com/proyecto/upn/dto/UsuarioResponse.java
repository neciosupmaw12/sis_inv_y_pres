package com.proyecto.upn.dto;

import com.proyecto.upn.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario del sistema")
public record UsuarioResponse(Long id, String username, String nombre, String rol, Boolean activo) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombre(), u.getRol().name(), u.getActivo());
    }
}
