package com.enlaza.factura.dto;

import com.enlaza.factura.EstadoFactura;
import com.enlaza.factura.Factura;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FacturaResponse(
        UUID id,
        UUID serieId,
        int numero,
        Instant fechaEmision,
        UUID clienteId,
        EstadoFactura estado,
        String hashAnterior,
        String hashActual,
        BigDecimal baseImponible,
        BigDecimal totalIva,
        BigDecimal totalIrpf,
        BigDecimal total,
        UUID creadoPor,
        List<LineaFacturaResponse> lineas
) {
    public static FacturaResponse of(Factura factura, List<LineaFacturaResponse> lineas) {
        return new FacturaResponse(
                factura.getId(),
                factura.getSerieId(),
                factura.getNumero(),
                factura.getFechaEmision(),
                factura.getClienteId(),
                factura.getEstado(),
                factura.getHashAnterior(),
                factura.getHashActual(),
                factura.getBaseImponible(),
                factura.getTotalIva(),
                factura.getTotalIrpf(),
                factura.getTotal(),
                factura.getCreadoPor(),
                lineas
        );
    }
}
