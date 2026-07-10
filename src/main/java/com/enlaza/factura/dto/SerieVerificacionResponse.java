package com.enlaza.factura.dto;

import java.util.UUID;

public record SerieVerificacionResponse(
        UUID serieId,
        int totalFacturas,
        boolean integra,
        UUID facturaId,
        Integer numero,
        String motivo
) {
    public static SerieVerificacionResponse integra(UUID serieId, int totalFacturas) {
        return new SerieVerificacionResponse(serieId, totalFacturas, true, null, null, null);
    }

    public static SerieVerificacionResponse rota(UUID serieId, int totalFacturas, UUID facturaId, int numero, String motivo) {
        return new SerieVerificacionResponse(serieId, totalFacturas, false, facturaId, numero, motivo);
    }
}
