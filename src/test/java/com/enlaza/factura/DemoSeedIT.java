package com.enlaza.factura;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.empresa.Empresa;
import com.enlaza.empresa.EmpresaRepository;
import com.enlaza.serie.SerieFacturacionService;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El DemoDataSeeder debe dejar la empresa demo con una serie y facturas
 * reales (pasan por FacturaService), de modo que la demo publica no arranca
 * vacia y su cadena de hashes es verificable desde el primer momento.
 */
class DemoSeedIT extends AbstractIntegrationTest {

    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private SerieFacturacionService serieService;
    @Autowired
    private FacturaService facturaService;
    @Autowired
    private SerieVerificacionService verificacionService;

    @Test
    void seeder_siembraSerieConTresFacturasConsecutivas() {
        Empresa demo = empresaRepository.findAll().stream()
                .filter(e -> e.getNif().equals("B00000000")).findFirst().orElseThrow();
        SerieFacturacionResponse serie = serieService.listar(demo.getId()).stream()
                .filter(s -> s.codigo().equals("A")).findFirst().orElseThrow();

        var facturas = facturaService.listar(demo.getId(), serie.id());

        assertThat(facturas).hasSize(3);
        assertThat(facturas).extracting(f -> f.numero()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(verificacionService.verificar(demo.getId(), serie.id()).integra()).isTrue();
    }
}
