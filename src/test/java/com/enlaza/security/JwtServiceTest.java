package com.enlaza.security;

import com.enlaza.usuario.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test unitario puro (sin Spring) de JwtService: no tenia ninguna cobertura
 * directa pese a ser la pieza de seguridad mas sensible del proyecto (ver el
 * bug de concurrencia real de RefreshTokenService en la Fase 3).
 */
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void generarYLuegoExtraer_devuelveElMismoUserId() {
        JwtService jwtService = jwtService(15);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "user@test.enlaza.dev", Role.ADMIN);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void tokenExpirado_lanzaJwtException() {
        JwtService jwtService = jwtService(-1);
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "user@test.enlaza.dev", Role.CONTABLE);

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFirmadoConOtroSecreto_esRechazado() {
        JwtService emisor = jwtService(15);
        String token = emisor.generateAccessToken(UUID.randomUUID(), "user@test.enlaza.dev", Role.CLIENTE);

        JwtService verificadorConOtroSecreto = jwtService(15,
                Base64.getEncoder().encodeToString("otro-secreto-distinto-de-32-bytes".substring(0, 32).getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> verificadorConOtroSecreto.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenMalformado_lanzaJwtException() {
        JwtService jwtService = jwtService(15);

        assertThatThrownBy(() -> jwtService.extractUserId("esto-no-es-un-jwt")).isInstanceOf(JwtException.class);
    }

    private JwtService jwtService(long accessTokenExpirationMinutes) {
        return jwtService(accessTokenExpirationMinutes, SECRET);
    }

    private JwtService jwtService(long accessTokenExpirationMinutes, String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setAccessTokenExpirationMinutes(accessTokenExpirationMinutes);
        properties.setRefreshTokenExpirationDays(30);
        return new JwtService(properties);
    }
}
