package com.enlaza.factura;

import com.enlaza.factura.dto.LineaFacturaRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario puro (sin Spring/Testcontainers) del calculo de impuestos por
 * linea: calcularLinea() no toca ningun repositorio, asi que se instancia
 * FacturaService con dependencias null para aislar solo esta logica. El
 * roadmap original de la Fase 7 pedia priorizar impuestos explicitamente.
 */
class FacturaServiceCalcularLineaTest {

    private final FacturaService facturaService = new FacturaService(null, null, null, null, null, null);

    @Test
    void lineaConIvaEIrpf_calculaBaseIvaIrpfYTotal() {
        LineaFactura linea = calcular("Servicio", "2.00", "100.00", "21.00", "15.00");

        assertThat(linea.getImporteBase()).isEqualByComparingTo("200.00");
        assertThat(linea.getImporteIva()).isEqualByComparingTo("42.00");
        assertThat(linea.getImporteIrpf()).isEqualByComparingTo("30.00");
        assertThat(linea.getImporteTotal()).isEqualByComparingTo("212.00");
    }

    @Test
    void irpfCero_elTotalEsBaseMasIva() {
        LineaFactura linea = calcular("Producto", "1.00", "50.00", "10.00", "0.00");

        assertThat(linea.getImporteIrpf()).isEqualByComparingTo("0.00");
        assertThat(linea.getImporteTotal()).isEqualByComparingTo("55.00");
    }

    @Test
    void dosLineasConIvaEIrpfDistintos_cadaUnaRedondeaPorSuCuenta() {
        LineaFactura linea1 = calcular("A", "1.00", "10.00", "21.00", "15.00");
        LineaFactura linea2 = calcular("B", "1.00", "10.00", "10.00", "0.00");

        assertThat(linea1.getImporteIva()).isEqualByComparingTo("2.10");
        assertThat(linea1.getImporteIrpf()).isEqualByComparingTo("1.50");
        assertThat(linea2.getImporteIva()).isEqualByComparingTo("1.00");
        assertThat(linea2.getImporteIrpf()).isEqualByComparingTo("0.00");
    }

    /**
     * cantidad=1.00 x precioUnitario=33.30 = importeBase 33.30; con
     * porcentajeIva=15.00 el calculo crudo es 33.30*15/100 = 4.995, que cae
     * EXACTAMENTE en el limite entre 4.99 y 5.00 (el tercer decimal es un 5
     * seco, no un 9 seguido de un 5 -- eso ya no seria un empate real y
     * redondearia igual bajo cualquier modo). HALF_UP debe redondear hacia
     * arriba (aleja de cero); HALF_DOWN redondearia hacia abajo, asi que
     * este caso si distingue el modo de redondeo usado.
     */
    @Test
    void redondeoHalfUp_enElLimiteExactoDeLaDivisionDeIva() {
        LineaFactura linea = calcular("Servicio", "1.00", "33.30", "15.00", "0.00");

        assertThat(linea.getImporteIva()).isEqualByComparingTo("5.00");
    }

    /**
     * calcularLinea() se llama aqui sin pasar por la validacion @Digits del
     * DTO (que en el flujo HTTP real rechazaria mas de 2 decimales), asi que
     * este test aisla directamente la normalizacion HALF_UP de cantidad
     * ANTES de multiplicar, que es la que evita la regresion de doble
     * redondeo corregida en la Fase 6.
     */
    @Test
    void cantidadConMasDeDosDecimales_seNormalizaAntesDeMultiplicar() {
        LineaFactura linea = calcular("Servicio", "1.005", "10.00", "0.00", "0.00");

        assertThat(linea.getCantidad()).isEqualByComparingTo("1.01");
        assertThat(linea.getImporteBase()).isEqualByComparingTo("10.10");
    }

    private LineaFactura calcular(String descripcion, String cantidad, String precioUnitario, String iva, String irpf) {
        return facturaService.calcularLinea(new LineaFacturaRequest(descripcion,
                new BigDecimal(cantidad), new BigDecimal(precioUnitario), new BigDecimal(iva), new BigDecimal(irpf)));
    }
}
