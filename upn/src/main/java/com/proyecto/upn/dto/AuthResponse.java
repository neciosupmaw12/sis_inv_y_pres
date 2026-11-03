package com.proyecto.upn.dto;

public record AuthResponse(String token, String tipo, String username, String nombre, String rol) {
}
