package com.enlaza.factura;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.factura.dto.LineaFacturaRequest;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usa la Empresa/Cliente demo que DemoDataSeeder ya vincula al usuario
 * semilla CLIENTE (via Usuario.clienteId) -- no hay endpoint publico para
 * crear ese vinculo todavia, es solo el seeder.
 */
class MisFacturasControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";
    private static final String CLIENTE_EMAIL = "cliente@test.enlaza.dev";
    private static final String CLIENTE_PASSWORD = "ClienteTest123!";
    private static final String EMPRESA_DEMO_NIF = "B00000000";
    private static final String CLIENTE_DEMO_NIF = "12345678Z";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void clienteAutenticado_veSuPropiaFacturaEnMisFacturas() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        UUID empresaDemoId = buscarEmpresaDemo(adminToken);
        UUID clienteDemoId = buscarClienteDemo(adminToken, empresaDemoId);

        UUID serieId = restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series", HttpMethod.POST,
                authenticated(adminToken, new SerieFacturacionRequest("MISFAC-" + UUID.randomUUID().toString().substring(0, 6))),
                SerieFacturacionResponse.class).getBody().id();

        FacturaRequest facturaRequest = new FacturaRequest(clienteDemoId, List.of(
                new LineaFacturaRequest("Servicio demo", BigDecimal.ONE, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO)));
        FacturaResponse facturaCreada = restTemplate.exchange(
                "/api/empresas/" + empresaDemoId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                authenticated(adminToken, facturaRequest), FacturaResponse.class).getBody();

        String clienteToken = login(CLIENTE_EMAIL, CLIENTE_PASSWORD);
        ResponseEntity<FacturaResponse[]> misFacturas = restTemplate.exchange(
                "/api/mis-facturas", HttpMethod.GET, authenticated(clienteToken), FacturaResponse[].class);

        assertThat(misFacturas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(misFacturas.getBody()).extracting(FacturaResponse::id).contains(facturaCreada.id());
    }

    @Test
    void adminNoPuedeUsarElEndpointDeMisFacturas() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mis-facturas", HttpMethod.GET, authenticated(adminToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private UUID buscarEmpresaDemo(String adminToken) {
        EmpresaResponse[] empresas = restTemplate.exchange(
                "/api/empresas", HttpMethod.GET, authenticated(adminToken), EmpresaResponse[].class).getBody();
        return java.util.Arrays.stream(empresas)
                .filter(e -> e.nif().equals(EMPRESA_DEMO_NIF))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private UUID buscarClienteDemo(String adminToken, UUID empresaId) {
        ClienteResponse[] clientes = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/clientes", HttpMethod.GET, authenticated(adminToken), ClienteResponse[].class).getBody();
        return java.util.Arrays.stream(clientes)
                .filter(c -> c.nif().equals(CLIENTE_DEMO_NIF))
                .findFirst()
                .orElseThrow()
                .id();
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
