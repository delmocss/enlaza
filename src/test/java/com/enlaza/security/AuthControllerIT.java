package com.enlaza.security;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.security.dto.ChangePasswordRequest;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.security.dto.LogoutRequest;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    /**
     * Regresion del bug encontrado durante la revision de codigo de esta
     * fase: RefreshTokenService.revokeAllForUser() se llamaba desde rotate()
     * como self-invocation (this.revokeAllForUser(...)), que Spring no
     * intercepta con el proxy @Transactional -- la revocacion vivia dentro
     * de la MISMA transaccion que rotate(), y el rollback-on-exception de
     * BadCredentialsException la deshacia en silencio antes de llegar a
     * Postgres. Efecto real: al reutilizar un token ya rotado (robo/reuso),
     * el sucesor legitimo NO se revocaba pese a que el codigo aparentaba
     * hacerlo. Corregido con una sub-transaccion REQUIRES_NEW invocada a
     * traves de una auto-inyeccion perezosa del propio bean (ver
     * RefreshTokenService.self). Este test confirma el efecto de verdad:
     * reutilizar el token original debe tumbar tambien al sucesor.
     */
    @Test
    void reutilizarUnTokenYaRotado_revocaTambienElSucesorLegitimo() {
        LoginResponse original = loginAndGetTokens(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<LoginResponse> primeraRotacion = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(original.refreshToken()), LoginResponse.class);
        assertThat(primeraRotacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        String tokenSucesor = primeraRotacion.getBody().refreshToken();

        ResponseEntity<String> reusoDelOriginal = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(original.refreshToken()), String.class);
        assertThat(reusoDelOriginal.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> sucesorTrasElReuso = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(tokenSucesor), String.class);
        assertThat(sucesorTrasElReuso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Regresion del bug TOCTOU corregido en la Fase 3: el test secuencial de
     * arriba (refresh_rotatesToken_andRejectsReuseOfOldToken) solo prueba el
     * camino feliz de "usar, luego reusar". Este test dispara el mismo
     * refresh token desde N hilos A LA VEZ (mismo patron de
     * FacturaControllerIT.creacionConcurrente) para confirmar que el UPDATE
     * condicional atomico de RefreshTokenService.rotate() deja ganar a
     * exactamente uno bajo carga real, no solo en un escenario secuencial
     * donde la carrera nunca llega a manifestarse.
     */
    @Test
    void refreshConcurrente_conElMismoToken_soloUnoTieneExito() throws InterruptedException {
        LoginResponse tokens = loginAndGetTokens(ADMIN_EMAIL, ADMIN_PASSWORD);

        int hilos = 10;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<ResponseEntity<String>> resultados = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < hilos; i++) {
                executor.submit(() -> {
                    listos.countDown();
                    try {
                        salida.await();
                        resultados.add(restTemplate.postForEntity(
                                "/api/auth/refresh", new RefreshRequest(tokens.refreshToken()), String.class));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            listos.await(5, TimeUnit.SECONDS);
            salida.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(resultados).hasSize(hilos);
        long exitosos = resultados.stream().filter(r -> r.getStatusCode() == HttpStatus.OK).count();
        long rechazados = resultados.stream().filter(r -> r.getStatusCode() == HttpStatus.UNAUTHORIZED).count();

        assertThat(exitosos).isEqualTo(1);
        assertThat(rechazados).isEqualTo(hilos - 1);
    }

    @Test
    void logout_revocaElRefreshToken_yUnRefreshPosteriorEsRechazado() {
        LoginResponse tokens = loginAndGetTokens(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/api/auth/logout", new LogoutRequest(tokens.refreshToken()), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refreshTrasLogout = restTemplate.postForEntity(
                "/api/auth/refresh", new RefreshRequest(tokens.refreshToken()), String.class);
        assertThat(refreshTrasLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
