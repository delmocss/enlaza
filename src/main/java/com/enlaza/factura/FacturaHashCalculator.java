package com.enlaza.factura;

import com.enlaza.common.hash.GenesisHash;
import com.enlaza.common.hash.Sha256;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Formula unica del hash de una Factura, compartida por FacturaService
 * (creacion, Fase 5) y SerieVerificacionService (verificacion, Fase 6).
 * Extraida para que ambos lados de la cadena de integridad NUNCA puedan
 * divergir: si se duplicara la formula, un cambio futuro en un solo sitio
 * rompería la verificacion en silencio.
 *
 * Normaliza internamente la escala de los BigDecimal (2 decimales) y la
 * precision del Instant (milisegundos) en vez de confiar en que cada
 * llamador lo haga: Postgres persiste NUMERIC/TIMESTAMP con su propia
 * escala/precision, y una relectura para verificar debe coincidir siempre
 * con lo que se hasheo al crear. Centralizar esto aqui (en vez de en
 * FacturaService, como en la primera version de esta correccion) evita que
 * un futuro llamador (importacion masiva, factura rectificativa, un
 * script de migracion) reintroduzca el mismo bug por no replicar la
 * normalizacion a mano.
 *
 * NO incluye Factura.estado a proposito: anular() preserva
 * deliberadamente el hash para no romper la cadena hacia adelante (ver
 * comentario de Factura), asi que esta cadena no esta pensada para
 * detectar manipulaciones del estado EMITIDA/ANULADA -- eso le
 * corresponderia a un futuro AuditLog (deuda ya identificada del
 * proyecto), no a esta formula.
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
                        escapar(l.getDescripcion()),
                        normalizar(l.getCantidad()).toPlainString(),
                        normalizar(l.getPrecioUnitario()).toPlainString(),
                        normalizar(l.getPorcentajeIva()).toPlainString(),
                        normalizar(l.getPorcentajeIrpf()).toPlainString(),
                        normalizar(l.getImporteTotal()).toPlainString()))
                .collect(Collectors.joining(";"));

        String contenido = String.join("|",
                serieId.toString(),
                String.valueOf(numero),
                fechaEmision.truncatedTo(ChronoUnit.MILLIS).toString(),
                clienteId.toString(),
                normalizar(baseImponible).toPlainString(),
                normalizar(totalIva).toPlainString(),
                normalizar(totalIrpf).toPlainString(),
                normalizar(total).toPlainString(),
                lineasContenido,
                hashAnterior);
        return Sha256.hex(contenido);
    }

    /**
     * Escapa el caracter de escape y los dos delimitadores usados arriba
     * (":" entre campos de una linea, ";" entre lineas) dentro de un campo
     * de texto libre. Sin esto, una "descripcion" fabricada con esos
     * caracteres (p. ej. "A:1.00:2.00:0.00:0.00:2.00;B") podria hacer que
     * una unica linea produjera el mismo lineasContenido -- y por tanto el
     * mismo hash -- que varias lineas genuinas distintas, permitiendo
     * sustituir el contenido real de una factura sin que la verificacion
     * lo detectara.
     */
    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace(":", "\\:").replace(";", "\\;");
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
