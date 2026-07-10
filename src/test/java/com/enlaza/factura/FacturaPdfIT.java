package com.enlaza.factura;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.factura.dto.LineaFacturaRequest;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usa la Empresa/Cliente demo (misma que MisFacturasControllerIT) para
 * poder ejercer tanto el endpoint empresa-scoped (ADMIN/CONTABLE) como el
 * de /api/mis-facturas (CLIENTE) sobre la misma factura.
 */
class FacturaPdfIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";
    private static final String CLIENTE_EMAIL = "cliente@test.enlaza.dev";
    private static final String CLIENTE_PASSWORD = "ClienteTest123!";
    private static final String EMPRESA_DEMO_NIF = "B00000000";
    private static final String CLIENTE_DEMO_NIF = "12345678Z";

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken;
    private UUID empresaDemoId;
    private String empresaDemoRazonSocial;
    private UUID clienteDemoId;
    private String clienteDemoNombre;
    private UUID serieId;
    private String serieCodigo;

    @BeforeEach
    void setUp() {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        EmpresaResponse empresaDemo = buscarEmpresaDemo();
        empresaDemoId = empresaDemo.id();
        empresaDemoRazonSocial = empresaDemo.razonSocial();
        ClienteResponse clienteDemo = buscarClienteDemo(empresaDemoId);
        clienteDemoId = clienteDemo.id();
        clienteDemoNombre = clienteDemo.nombre();

        serieCodigo = "PDF-" + UUID.randomUUID().toString().substring(0, 6);
        serieId = restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series", HttpMethod.POST,
                authenticated(adminToken, new SerieFacturacionRequest(serieCodigo)),
                SerieFacturacionResponse.class).getBody().id();
    }

    @Test
    void adminDescargaElPdfDeUnaFacturaEmpresaScoped() {
        FacturaResponse factura = crearFactura(clienteDemoId);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series/" + serieId + "/facturas/" + factura.id() + "/pdf",
                HttpMethod.GET, authenticated(adminToken), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(esUnPdfValido(response.getBody())).isTrue();

        String texto = extraerTexto(response.getBody());
        // Se usa un prefijo del hash, no el valor completo: a 8px de fuente
        // los 64 caracteres hexadecimales podrian quedar partidos en dos
        // lineas por el renderer de PDF (word-wrap: break-word), lo que
        // rompe una busqueda de substring exacta sobre el texto extraido.
        assertThat(texto)
                .contains(empresaDemoRazonSocial)
                .contains(clienteDemoNombre)
                .contains(serieCodigo + "-" + factura.numero())
                .contains(factura.hashActual().substring(0, 16))
                .contains(factura.total().toString())
                .doesNotContain("ANULADA");
    }

    @Test
    void clienteDescargaElPdfDeSuPropiaFactura() {
        FacturaResponse factura = crearFactura(clienteDemoId);
        String clienteToken = login(CLIENTE_EMAIL, CLIENTE_PASSWORD);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/mis-facturas/" + factura.id() + "/pdf", HttpMethod.GET, authenticated(clienteToken), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(esUnPdfValido(response.getBody())).isTrue();
        assertThat(extraerTexto(response.getBody())).contains(factura.hashActual().substring(0, 16));
    }

    @Test
    void clienteNoPuedeDescargarUnaFacturaDeOtroCliente() {
        UUID otroClienteId = restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/clientes", HttpMethod.POST,
                authenticated(adminToken, new ClienteRequest("Otro Cliente", "C" + UUID.randomUUID().toString().substring(0, 8), null, null)),
                ClienteResponse.class).getBody().id();
        FacturaResponse facturaAjena = crearFactura(otroClienteId);

        String clienteToken = login(CLIENTE_EMAIL, CLIENTE_PASSWORD);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mis-facturas/" + facturaAjena.id() + "/pdf", HttpMethod.GET, authenticated(clienteToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void elPdfDeUnaFacturaAnuladaMuestraLaEtiqueta() {
        FacturaResponse factura = crearFactura(clienteDemoId);
        restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series/" + serieId + "/facturas/" + factura.id() + "/anular",
                HttpMethod.PATCH, authenticated(adminToken), FacturaResponse.class);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series/" + serieId + "/facturas/" + factura.id() + "/pdf",
                HttpMethod.GET, authenticated(adminToken), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extraerTexto(response.getBody())).contains("ANULADA");
    }

    private boolean esUnPdfValido(byte[] contenido) {
        return contenido != null && contenido.length > 4
                && new String(contenido, 0, 4, StandardCharsets.US_ASCII).equals("%PDF");
    }

    private String extraerTexto(byte[] pdf) {
        try (PDDocument documento = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(documento);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private FacturaResponse crearFactura(UUID clienteId) {
        FacturaRequest request = new FacturaRequest(clienteId, List.of(
                new LineaFacturaRequest("Servicio demo", BigDecimal.ONE, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO)));
        return restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                authenticated(adminToken, request), FacturaResponse.class).getBody();
    }

    private EmpresaResponse buscarEmpresaDemo() {
        EmpresaResponse[] empresas = restTemplate.exchange(
                "/api/empresas", HttpMethod.GET, authenticated(adminToken), EmpresaResponse[].class).getBody();
        return Arrays.stream(empresas)
                .filter(e -> e.nif().equals(EMPRESA_DEMO_NIF))
                .findFirst().orElseThrow();
    }

    private ClienteResponse buscarClienteDemo(UUID empresaId) {
        ClienteResponse[] clientes = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/clientes", HttpMethod.GET, authenticated(adminToken), ClienteResponse[].class).getBody();
        return Arrays.stream(clientes)
                .filter(c -> c.nif().equals(CLIENTE_DEMO_NIF))
                .findFirst().orElseThrow();
    }

    private String login(String email, String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, password), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().accessToken();
    }

    private HttpEntity<Void> authenticated(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authenticated(String accessToken, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }
}
