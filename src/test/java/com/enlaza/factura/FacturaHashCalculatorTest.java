package com.enlaza.factura;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario puro (sin Spring/Testcontainers) de la formula de hash: es
 * logica sin dependencia de BD, asi que aqui se verifica en milisegundos lo
 * que en SerieVerificacionIT se prueba indirectamente via manipulacion HTTP.
 */
class FacturaHashCalculatorTest {

    private static final UUID SERIE_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final Instant FECHA = Instant.parse("2026-07-12T10:00:00Z");
    private static final String HASH_ANTERIOR = FacturaHashCalculator.genesis();

    @Test
    void genesis_esElHashSha256DeLaSemillaFija() {
        assertThat(FacturaHashCalculator.genesis()).hasSize(64).matches("[0-9a-f]+");
        assertThat(FacturaHashCalculator.genesis()).isEqualTo(FacturaHashCalculator.genesis());
    }

    @Test
    void mismosDatos_producenSiempreElMismoHash() {
        List<LineaFactura> lineas = List.of(linea("Servicio", "2.00", "100.00", "21.00", "15.00", "212.00"));

        String hash1 = calcular(lineas);
        String hash2 = calcular(lineas);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void cambiarUnaLineaCambiaElHash() {
        List<LineaFactura> original = List.of(linea("Servicio", "2.00", "100.00", "21.00", "15.00", "212.00"));
        List<LineaFactura> modificada = List.of(linea("Servicio", "2.00", "999.00", "21.00", "15.00", "212.00"));

        assertThat(calcular(original)).isNotEqualTo(calcular(modificada));
    }

    /**
     * Regresion del hallazgo mas grave de la Fase 6: sin escapar ":" y ";"
     * dentro de "descripcion", una unica linea fabricada podia producir el
     * mismo "lineasContenido" -- y por tanto el mismo hash -- que varias
     * lineas genuinas distintas.
     */
    @Test
    void descripcionFabricadaConDelimitadores_noColisionaConVariasLineasGenuinas() {
        LineaFactura lineaUnicaFabricada = linea(
                "A:1.00:2.00:0.00:0.00:2.00;B", "1.00", "1.00", "0.00", "0.00", "1.00");
        LineaFactura lineaA = linea("A", "1.00", "2.00", "0.00", "0.00", "2.00");
        LineaFactura lineaB = linea("B", "1.00", "1.00", "0.00", "0.00", "1.00");

        String hashFabricado = calcular(List.of(lineaUnicaFabricada));
        String hashGenuino = calcular(List.of(lineaA, lineaB));

        assertThat(hashFabricado).isNotEqualTo(hashGenuino);
    }

    @Test
    void escalaDistintaDelMismoValorNumerico_produceElMismoHash() {
        List<LineaFactura> escalaCruda = List.of(linea("Servicio", "1", "10", "0", "0", "10"));
        List<LineaFactura> escalaNormalizada = List.of(linea("Servicio", "1.00", "10.00", "0.00", "0.00", "10.00"));

        assertThat(calcular(escalaCruda)).isEqualTo(calcular(escalaNormalizada));
    }

    @Test
    void precisionDeNanosegundos_seTruncaAMilisegundosComoAlPersistir() {
        List<LineaFactura> lineas = List.of(linea("Servicio", "1.00", "10.00", "0.00", "0.00", "10.00"));
        Instant conNanos = FECHA.plusNanos(123_456);
        Instant truncada = conNanos.truncatedTo(ChronoUnit.MILLIS);

        String hashConNanos = FacturaHashCalculator.calcular(SERIE_ID, 1, conNanos, CLIENTE_ID,
                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"), lineas, HASH_ANTERIOR);
        String hashTruncado = FacturaHashCalculator.calcular(SERIE_ID, 1, truncada, CLIENTE_ID,
                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"), lineas, HASH_ANTERIOR);

        assertThat(hashConNanos).isEqualTo(hashTruncado);
    }

    @Test
    void hashAnteriorDistinto_produceHashActualDistinto() {
        List<LineaFactura> lineas = List.of(linea("Servicio", "1.00", "10.00", "0.00", "0.00", "10.00"));

        String hashConGenesis = calcular(lineas, HASH_ANTERIOR);
        String hashConOtroAnterior = calcular(lineas, "otro-hash-anterior-distinto");

        assertThat(hashConGenesis).isNotEqualTo(hashConOtroAnterior);
    }

    private String calcular(List<LineaFactura> lineas) {
        return calcular(lineas, HASH_ANTERIOR);
    }

    private String calcular(List<LineaFactura> lineas, String hashAnterior) {
        return FacturaHashCalculator.calcular(SERIE_ID, 1, FECHA, CLIENTE_ID,
                new BigDecimal("200.00"), new BigDecimal("42.00"), new BigDecimal("30.00"), new BigDecimal("212.00"),
                lineas, hashAnterior);
    }

    private LineaFactura linea(String descripcion, String cantidad, String precioUnitario,
                                String iva, String irpf, String importeTotal) {
        return LineaFactura.builder()
                .descripcion(descripcion)
                .cantidad(new BigDecimal(cantidad))
                .precioUnitario(new BigDecimal(precioUnitario))
                .porcentajeIva(new BigDecimal(iva))
                .porcentajeIrpf(new BigDecimal(irpf))
                .importeTotal(new BigDecimal(importeTotal))
                .build();
    }
}
