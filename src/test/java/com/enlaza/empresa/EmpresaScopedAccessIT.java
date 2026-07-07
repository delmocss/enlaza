package com.enlaza.empresa;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.security.dto.MeResponse;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre EmpresaAccessService: lo que realmente distingue esta fase es que
 * el rol CONTABLE no basta por si solo -- el acceso a Cliente y
 * SerieFacturacion depende de la asignacion via UsuarioEmpresa, dato por
 * dato, no solo por rol.
 */
class EmpresaScopedAccessIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";
    private static final String CONTABLE_EMAIL = "contable@test.enlaza.dev";
    private static final String CONTABLE_PASSWORD = "ContableTest123!";

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
    void contableAsignado_puedeCrearYListarClientes() {
        ResponseEntity<ClienteResponse> creado = restTemplate.exchange(
                "/api/empresas/" + empresaAsignada + "/clientes", HttpMethod.POST,
                authenticated(contableToken, nuevoClienteRequest()), ClienteResponse.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ClienteResponse[]> listado = restTemplate.exchange(
                "/api/empresas/" + empresaAsignada + "/clientes", HttpMethod.GET,
                authenticated(contableToken), ClienteResponse[].class);
        assertThat(listado.getBody()).hasSize(1);
    }

    @Test
    void contableNoAsignado_recibe403AlOperarClientes() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaNoAsignada + "/clientes", HttpMethod.POST,
                authenticated(contableToken, nuevoClienteRequest()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_puedeOperarClientesDeCualquierEmpresa() {
        ResponseEntity<ClienteResponse> response = restTemplate.exchange(
                "/api/empresas/" + empresaNoAsignada + "/clientes", HttpMethod.POST,
                authenticated(adminToken, nuevoClienteRequest()), ClienteResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void contableAsignado_puedeCrearSerieConHashGenesis() {
        ResponseEntity<SerieFacturacionResponse> response = restTemplate.exchange(
                "/api/empresas/" + empresaAsignada + "/series", HttpMethod.POST,
                authenticated(contableToken, new SerieFacturacionRequest("A")), SerieFacturacionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SerieFacturacionResponse serie = response.getBody();
        assertThat(serie.ultimoNumero()).isZero();
        assertThat(serie.activa()).isTrue();
        assertThat(serie.ultimoHash()).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void contableNoAsignado_recibe403AlCrearSerie() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaNoAsignada + "/series", HttpMethod.POST,
                authenticated(contableToken, new SerieFacturacionRequest("A")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crearSerie_conCodigoDuplicadoEnMismaEmpresa_devuelve409() {
        restTemplate.exchange(
                "/api/empresas/" + empresaAsignada + "/series", HttpMethod.POST,
                authenticated(adminToken, new SerieFacturacionRequest("F2026")), SerieFacturacionResponse.class);

        ResponseEntity<String> duplicada = restTemplate.exchange(
                "/api/empresas/" + empresaAsignada + "/series", HttpMethod.POST,
                authenticated(adminToken, new SerieFacturacionRequest("F2026")), String.class);

        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private ResponseEntity<EmpresaResponse> crearEmpresa(String token) {
        EmpresaRequest request = new EmpresaRequest("Empresa Test " + UUID.randomUUID(), "T" + UUID.randomUUID().toString().substring(0, 8), null, null, null);
        return restTemplate.exchange("/api/empresas", HttpMethod.POST, authenticated(token, request), EmpresaResponse.class);
    }

    private ClienteRequest nuevoClienteRequest() {
        return new ClienteRequest("Cliente Test " + UUID.randomUUID(), "C" + UUID.randomUUID().toString().substring(0, 8), null, null);
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
