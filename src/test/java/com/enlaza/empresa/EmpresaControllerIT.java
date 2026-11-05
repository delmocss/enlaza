package com.enlaza.empresa;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
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

class EmpresaControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";
    private static final String CONTABLE_EMAIL = "contable@test.enlaza.dev";
    private static final String CONTABLE_PASSWORD = "ContableTest123!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void crear_comoAdmin_devuelve201() {
        ResponseEntity<EmpresaResponse> response = crearEmpresa(adminToken(), nuevaEmpresaRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().activa()).isTrue();
    }

    @Test
    void crear_comoContable_devuelve403() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas", HttpMethod.POST,
                authenticated(contableToken(), nuevaEmpresaRequest()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_conNifDuplicado_devuelve409() {
        EmpresaRequest request = nuevaEmpresaRequest();
        crearEmpresa(adminToken(), request);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas", HttpMethod.POST, authenticated(adminToken(), request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void obtener_empresaInexistente_devuelve404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + UUID.randomUUID(), HttpMethod.GET, authenticated(adminToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtener_conIdMalformado_devuelve400_noUn500() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/no-es-un-uuid", HttpMethod.GET, authenticated(adminToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void actualizar_comoAdmin_actualizaCampos() {
        UUID id = crearEmpresa(adminToken(), nuevaEmpresaRequest()).getBody().id();
        EmpresaRequest actualizacion = new EmpresaRequest("Nueva Razon Social S.L.", nifUnico(), "Calle Nueva 1", "nuevo@empresa.dev", "600000000");

        ResponseEntity<EmpresaResponse> response = restTemplate.exchange(
                "/api/empresas/" + id, HttpMethod.PUT, authenticated(adminToken(), actualizacion), EmpresaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().razonSocial()).isEqualTo("Nueva Razon Social S.L.");
    }

    @Test
    void activarYDesactivar_cambianEstado() {
        UUID id = crearEmpresa(adminToken(), nuevaEmpresaRequest()).getBody().id();

        ResponseEntity<EmpresaResponse> desactivada = restTemplate.exchange(
                "/api/empresas/" + id + "/desactivar", HttpMethod.PATCH, authenticated(adminToken()), EmpresaResponse.class);
        assertThat(desactivada.getBody().activa()).isFalse();

        ResponseEntity<EmpresaResponse> reactivada = restTemplate.exchange(
                "/api/empresas/" + id + "/activar", HttpMethod.PATCH, authenticated(adminToken()), EmpresaResponse.class);
        assertThat(reactivada.getBody().activa()).isTrue();
    }

    private ResponseEntity<EmpresaResponse> crearEmpresa(String token, EmpresaRequest request) {
        return restTemplate.exchange("/api/empresas", HttpMethod.POST, authenticated(token, request), EmpresaResponse.class);
    }

    private EmpresaRequest nuevaEmpresaRequest() {
        return new EmpresaRequest("Empresa Test " + UUID.randomUUID(), nifUnico(), null, null, null);
    }

    private String nifUnico() {
        return "T" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String adminToken() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String contableToken() {
        return login(CONTABLE_EMAIL, CONTABLE_PASSWORD);
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
