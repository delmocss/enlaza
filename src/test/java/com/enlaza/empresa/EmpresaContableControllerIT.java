package com.enlaza.empresa;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.empresa.dto.ContableAsignadoResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmpresaContableController no tenia ninguna clase de test propia: se usaba
 * solo como setup interno en EmpresaScopedAccessIT para dejar un CONTABLE
 * asignado, nunca se ejercian sus propios casos (listar, duplicado, rol
 * incorrecto, desasignar, 404, 403).
 */
class EmpresaContableControllerIT extends AbstractIntegrationTest {

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
    private UUID empresaId;
    private UUID contableId;

    @BeforeEach
    void setUp() {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        contableToken = login(CONTABLE_EMAIL, CONTABLE_PASSWORD);
        contableId = me(contableToken).id();

        empresaId = restTemplate.exchange("/api/empresas", HttpMethod.POST,
                authenticated(adminToken, new EmpresaRequest("Empresa Contables " + UUID.randomUUID(),
                        "T" + UUID.randomUUID().toString().substring(0, 8), null, null, null)),
                EmpresaResponse.class).getBody().id();
    }

    @Test
    void asignar_luegoListar_devuelveElContable() {
        ResponseEntity<Void> asignado = asignar(empresaId, contableId);
        assertThat(asignado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ContableAsignadoResponse[]> listado = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables", HttpMethod.GET,
                authenticated(adminToken), ContableAsignadoResponse[].class);

        assertThat(listado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listado.getBody()).hasSize(1);
        assertThat(listado.getBody()[0].usuarioId()).isEqualTo(contableId);
        assertThat(listado.getBody()[0].email()).isEqualTo(CONTABLE_EMAIL);
    }

    @Test
    void asignarElMismoContableDosVeces_laSegundaDevuelve409() {
        assignAndAssertCreated();

        ResponseEntity<String> segunda = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables/" + contableId, HttpMethod.POST,
                authenticated(adminToken), String.class);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void asignarUnUsuarioConRolCliente_devuelve409() {
        UUID clienteUsuarioId = me(login(CLIENTE_EMAIL, CLIENTE_PASSWORD)).id();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables/" + clienteUsuarioId, HttpMethod.POST,
                authenticated(adminToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void asignarAUnaEmpresaInexistente_devuelve404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + UUID.randomUUID() + "/contables/" + contableId, HttpMethod.POST,
                authenticated(adminToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void desasignar_luegoListar_quedaVacio() {
        assignAndAssertCreated();

        ResponseEntity<Void> desasignado = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables/" + contableId, HttpMethod.DELETE,
                authenticated(adminToken), Void.class);
        assertThat(desasignado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ContableAsignadoResponse[]> listado = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables", HttpMethod.GET,
                authenticated(adminToken), ContableAsignadoResponse[].class);
        assertThat(listado.getBody()).isEmpty();
    }

    @Test
    void desasignarUnContableNoAsignado_devuelve404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables/" + contableId, HttpMethod.DELETE,
                authenticated(adminToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void comoContable_recibe403AlListarOAsignar() {
        ResponseEntity<String> listar = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables", HttpMethod.GET,
                authenticated(contableToken), String.class);
        assertThat(listar.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> asignar = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/contables/" + contableId, HttpMethod.POST,
                authenticated(contableToken), String.class);
        assertThat(asignar.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void assignAndAssertCreated() {
        ResponseEntity<Void> response = asignar(empresaId, contableId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<Void> asignar(UUID empresaId, UUID usuarioId) {
        return restTemplate.exchange("/api/empresas/" + empresaId + "/contables/" + usuarioId,
                HttpMethod.POST, authenticated(adminToken), Void.class);
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
