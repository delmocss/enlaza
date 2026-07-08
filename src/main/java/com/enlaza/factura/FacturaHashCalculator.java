package com.enlaza.factura;

import com.enlaza.common.hash.GenesisHash;
import com.enlaza.common.hash.Sha256;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Formula unica del hash de una Factura, compartida por FacturaService
 * (creacion, Fase 5) y SerieVerificacionService (verificacion, Fase 6).
 * Extraida para que ambos lados de la cadena de integridad NUNCA puedan
 * divergir: si se duplicara la formula, un cambio futuro en un solo sitio
 * rompería la verificacion en silencio.
 */
public final class FacturaHashCalculator {

    private FacturaHashCalculator() {
    }

    public static String genesis() {
        return GenesisHash.value();
    }

    /**
     * Incluye baseImponible/totalIva/totalIrpf y el contenido de cada linea
     * (no solo el total): dos facturas con el mismo total pero distinta
     * composicion fiscal o distintas lineas deben producir hashes distintos,
     * o alterar una linea directamente en la base de datos sin tocar el
     * total pasaria desapercibido para la cadena de integridad.
     */
    public static String calcular(UUID serieId, int numero, Instant fechaEmision, UUID clienteId,
                                   BigDecimal baseImponible, BigDecimal totalIva, BigDecimal totalIrpf, BigDecimal total,
                                   List<LineaFactura> lineas, String hashAnterior) {
        String lineasContenido = lineas.stream()
                .map(l -> String.join(":",
                        l.getDescripcion(),
                        l.getCantidad().toPlainString(),
                        l.getPrecioUnitario().toPlainString(),
                        l.getPorcentajeIva().toPlainString(),
                        l.getPorcentajeIrpf().toPlainString(),
                        l.getImporteTotal().toPlainString()))
                .collect(Collectors.joining(";"));

        String contenido = String.join("|",
                serieId.toString(),
                String.valueOf(numero),
                fechaEmision.toString(),
                clienteId.toString(),
                baseImponible.toPlainString(),
                totalIva.toPlainString(),
                totalIrpf.toPlainString(),
                total.toPlainString(),
                lineasContenido,
                hashAnterior);
        return Sha256.hex(contenido);
    }
}
