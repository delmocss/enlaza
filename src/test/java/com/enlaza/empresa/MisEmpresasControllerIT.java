package com.enlaza.empresa;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.security.dto.MeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET /api/mis-empresas: pensado para el selector de empresa del frontend,
 * distinto de GET /api/empresas (ADMIN-only). Un CONTABLE debe ver solo lo
 * asignado via UsuarioEmpresa, un ADMIN debe ver todo, un CLIENTE no entra.
 */
class MisEmpresasControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";
    private static final String CONTABLE_EMAIL = "contable@test.enlaza.dev";
    private static final String CONTABLE_PASSWORD = "ContableTest123!";
    private static final String CLIENTE_EMAIL = "cliente@test.enlaza.dev";
    private static final String CLIENTE_PASSWORD = "ClienteTest123!";

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken;
    private String contableToken;
    private UUID empresaAsignada;
    private UUID empresaNoAsignada;

    @BeforeEach
    void setUp() {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        contableToken = login(CONTABLE_EMAIL, CONTABLE_PASSWORD);

        empresaAsignada = crearEmpresa(adminToken).getBody().id();
        empresaNoAsignada = crearEmpresa(adminToken).getBody().id();

        UUID contableId = me(contableToken).id();
        ResponseEntity<Void> asignacion = restTemplate.exchange(
                "/api/empresas/" + empresaAsignada + "/contables/" + contableId,
                HttpMethod.POST, authenticated(adminToken), Void.class);
        assertThat(asignacion.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void contable_soloVeLasEmpresasAsignadas() {
        ResponseEntity<EmpresaResponse[]> response = restTemplate.exchange(
                "/api/mis-empresas", HttpMethod.GET, authenticated(contableToken), EmpresaResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<UUID> ids = List.of(response.getBody()).stream().map(EmpresaResponse::id).toList();
        assertThat(ids).contains(empresaAsignada).doesNotContain(empresaNoAsignada);
    }

    @Test
    void admin_veTodasLasEmpresas() {
        ResponseEntity<EmpresaResponse[]> response = restTemplate.exchange(
                "/api/mis-empresas", HttpMethod.GET, authenticated(adminToken), EmpresaResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<UUID> ids = List.of(response.getBody()).stream().map(EmpresaResponse::id).toList();
        assertThat(ids).contains(empresaAsignada, empresaNoAsignada);
    }

    @Test
    void cliente_recibe403() {
        String clienteToken = login(CLIENTE_EMAIL, CLIENTE_PASSWORD);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/mis-empresas", HttpMethod.GET, authenticated(clienteToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<EmpresaResponse> crearEmpresa(String token) {
        EmpresaRequest request = new EmpresaRequest("Empresa Test " + UUID.randomUUID(), nifUnico(), null, null, null);
        return restTemplate.exchange("/api/empresas", HttpMethod.POST, authenticated(token, request), EmpresaResponse.class);
    }

    private String nifUnico() {
        return "T" + UUID.randomUUID().toString().substring(0, 8);
    }

    private MeResponse me(String token) {
        ResponseEntity<MeResponse> response = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, authenticated(token), MeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
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
