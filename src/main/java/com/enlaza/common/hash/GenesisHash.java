package com.enlaza.common.hash;

/**
 * Semilla de la cadena de hashes de una SerieFacturacion nueva (ver D6/D10
 * del modelo de datos). Vive en common.hash, no en el paquete factura ni
 * serie, para que ambos puedan depender de ella sin crear una dependencia
 * ciclica entre esos dos paquetes: serie.SerieFacturacionService la usa al
 * sembrar una serie nueva, factura.FacturaHashCalculator la usa al verificar
 * que la primera factura de una serie enlaza contra el valor correcto.
 */
public final class GenesisHash {

    private static final String SEED = "ENLAZA-GENESIS";

    private GenesisHash() {
    }

    public static String value() {
        return Sha256.hex(SEED);
    }
}
