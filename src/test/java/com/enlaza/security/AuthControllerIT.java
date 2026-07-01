package com.enlaza.security;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.security.dto.ChangePasswordRequest;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.security.dto.MeResponse;
import com.enlaza.security.dto.RefreshRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void login_withSeededAdmin_succeeds() {
        ResponseEntity<LoginResponse> response = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
    }

    @Test
    void login_withWrongPassword_returns401() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(ADMIN_EMAIL, "wrong-password"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/auth/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withValidToken_returnsAdminRole() {
        LoginResponse tokens = loginAndGetTokens(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<MeResponse> response = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, authenticated(tokens.accessToken()), MeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(ADMIN_EMAIL);
        assertThat(response.getBody().role().name()).isEqualTo("ADMIN");
    }

    @Test
    void refresh_rotatesToken_andRejectsReuseOfOldToken() {
        LoginResponse tokens = loginAndGetTokens(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<LoginResponse> refreshed = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(tokens.refreshToken()), LoginResponse.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody()).isNotNull();
        assertThat(refreshed.getBody().refreshToken()).isNotEqualTo(tokens.refreshToken());

        ResponseEntity<String> reused = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(tokens.refreshToken()), String.class);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePassword_thenLoginWithNewPassword_succeeds() {
        String email = "contable@test.enlaza.dev";
        String originalPassword = "ContableTest123!";
        String newPassword = "NuevaClave456!";

        LoginResponse tokens = loginAndGetTokens(email, originalPassword);

        ResponseEntity<Void> changeResponse = restTemplate.exchange(
                "/api/auth/password", HttpMethod.PATCH,
                authenticated(tokens.accessToken(), new ChangePasswordRequest(originalPassword, newPassword)),
                Void.class);
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<LoginResponse> loginWithNewPassword = login(email, newPassword);
        assertThat(loginWithNewPassword.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<LoginResponse> login(String email, String password) {
        return restTemplate.postForEntity("/api/auth/login", new LoginRequest(email, password), LoginResponse.class);
    }

    /** Hace login y devuelve el cuerpo ya verificado como no nulo, para tests que solo necesitan los tokens como precondicion. */
    private LoginResponse loginAndGetTokens(String email, String password) {
        ResponseEntity<LoginResponse> response = login(email, password);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
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
