package com.enlaza.factura.dto;

import com.enlaza.factura.LineaFactura;

import java.math.BigDecimal;

public record LineaFacturaResponse(
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal porcentajeIva,
        BigDecimal porcentajeIrpf,
        BigDecimal importeBase,
        BigDecimal importeIva,
        BigDecimal importeIrpf,
        BigDecimal importeTotal
) {
    public static LineaFacturaResponse of(LineaFactura linea) {
        return new LineaFacturaResponse(
                linea.getDescripcion(),
                linea.getCantidad(),
                linea.getPrecioUnitario(),
                linea.getPorcentajeIva(),
                linea.getPorcentajeIrpf(),
                linea.getImporteBase(),
                linea.getImporteIva(),
                linea.getImporteIrpf(),
                linea.getImporteTotal()
        );
    }
}
