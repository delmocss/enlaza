package com.enlaza.empresa.dto;

import java.time.Instant;
import java.util.UUID;

public record ContableAsignadoResponse(
        UUID usuarioId,
        String email,
        String nombre,
        Instant fechaAsignacion
) {
}
