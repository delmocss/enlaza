package com.enlaza.cliente.dto;

import com.enlaza.cliente.Cliente;

import java.time.Instant;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        UUID empresaId,
        String nombre,
        String nif,
        String direccion,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClienteResponse of(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getEmpresaId(),
                cliente.getNombre(),
                cliente.getNif(),
                cliente.getDireccion(),
                cliente.getEmail(),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }
}
