package com.enlaza.security.dto;

import com.enlaza.usuario.Role;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String nombre,
        Role role
) {
}
