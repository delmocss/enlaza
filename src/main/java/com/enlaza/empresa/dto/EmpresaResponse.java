package com.enlaza.empresa.dto;

import com.enlaza.empresa.Empresa;

import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(
        UUID id,
        String razonSocial,
        String nif,
        String direccion,
        String email,
        String telefono,
        boolean activa,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmpresaResponse of(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getRazonSocial(),
                empresa.getNif(),
                empresa.getDireccion(),
                empresa.getEmail(),
                empresa.getTelefono(),
                empresa.isActiva(),
                empresa.getCreatedAt(),
                empresa.getUpdatedAt()
        );
    }
}
