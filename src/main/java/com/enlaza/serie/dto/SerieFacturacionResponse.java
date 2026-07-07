package com.enlaza.serie.dto;

import com.enlaza.serie.SerieFacturacion;

import java.time.Instant;
import java.util.UUID;

public record SerieFacturacionResponse(
        UUID id,
        UUID empresaId,
        String codigo,
        int ultimoNumero,
        String ultimoHash,
        boolean activa,
        Instant createdAt
) {
    public static SerieFacturacionResponse of(SerieFacturacion serie) {
        return new SerieFacturacionResponse(
                serie.getId(),
                serie.getEmpresaId(),
                serie.getCodigo(),
                serie.getUltimoNumero(),
                serie.getUltimoHash(),
                serie.isActiva(),
                serie.getCreatedAt()
        );
    }
}
