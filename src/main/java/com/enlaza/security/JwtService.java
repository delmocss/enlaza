package com.enlaza.security;

import com.enlaza.usuario.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Genera y valida unicamente el access token (JWT). El refresh token no pasa
 * por aqui -- ver RefreshTokenService.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

    /**
     * Recibe los datos ya resueltos (no la entidad Usuario) para que el
     * llamador pueda generar el token a partir del principal ya autenticado
     * sin tener que volver a consultar la base de datos.
     */
    public String generateAccessToken(UUID userId, String email, Role role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey())
                .compact();
    }

    /**
     * Extrae el id de usuario de un token ya validado. Lanza JwtException
     * (firma invalida, token expirado, formato incorrecto) si el token no es
     * de fiar -- el llamador decide si eso significa "no autenticado".
     */
    public UUID extractUserId(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationMinutes() * 60;
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }
}
