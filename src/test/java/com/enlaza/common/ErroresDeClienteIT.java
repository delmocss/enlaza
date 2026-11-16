package com.enlaza.common;

import com.enlaza.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los errores del cliente que Spring detecta antes de llegar al controlador
 * deben ser 4xx con el formato de error de la API, nunca un 500.
 */
class ErroresDeClienteIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void jsonMalformado_devuelve400NoOpaco500() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new HttpEntity<>("{bad", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("mal formada");
    }

    @Test
    void metodoHttpNoSoportado_devuelve405() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login", HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
