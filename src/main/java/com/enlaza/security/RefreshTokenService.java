package com.enlaza.security;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * El refresh token es una cadena aleatoria opaca, no un JWT: solo asi se
 * puede revocar y rotar de verdad desde el servidor (un refresh-JWT
 * necesitaria una lista negra para lograr lo mismo). Se hashea con SHA-256
 * antes de guardarlo -- no con BCrypt, porque BCrypt existe para ralentizar
 * el fuerza-bruta de secretos de baja entropia (contrasenas humanas); un
 * token aleatorio de 256 bits ya es imposible de adivinar, asi que un hash
 * rapido evita anadir latencia innecesaria en cada refresh.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService self;
    private final SecureRandom secureRandom = new SecureRandom();

    public record RotationResult(UUID usuarioId, String newRawToken) {
    }

    /**
     * self es una auto-inyeccion perezosa del propio bean: rotate() necesita
     * invocar revokeAllForUser() A TRAVES DEL PROXY de Spring (no como
     * this.revokeAllForUser(), una llamada directa de Java que el proxy no
     * intercepta) para que su @Transactional(REQUIRES_NEW) surta efecto de
     * verdad. @Lazy rompe el ciclo bean-necesita-su-propio-proxy-durante-su-
     * propia-creacion. Unica clase del proyecto con este patron porque es la
     * unica que necesita una sub-transaccion independiente desde dentro de
     * si misma.
     */
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties,
                                @Lazy RefreshTokenService self) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.self = self;
    }

    @Transactional
    public String issue(UUID usuarioId) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .usuarioId(usuarioId)
                .tokenHash(hash(rawToken))
                .expiryDate(Instant.now().plus(jwtProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));

        if (stored.isExpired()) {
            throw new BadCredentialsException("Refresh token expirado");
        }

        // UPDATE condicional atomico en vez de "leer revoked, luego escribir
        // true": comprobar y escribir en dos pasos separados es una carrera
        // real -- dos rotaciones concurrentes del mismo token verian ambas
        // revoked=false y ambas emitirian un hijo valido. El UPDATE
        // condicional hace que solo una gane; la otra se trata como reuso.
        int wonRace = refreshTokenRepository.revokeIfNotRevoked(stored.getId());
        if (wonRace == 0) {
            // Ya estaba revocado: o bien una rotacion concurrente se adelanto,
            // o es un token viejo reutilizado tras una rotacion anterior. En
            // ambos casos se trata como robo/reutilizacion por seguridad.
            self.revokeAllForUser(stored.getUsuarioId());
            throw new BadCredentialsException("Refresh token invalido o ya utilizado");
        }

        String newRawToken = issue(stored.getUsuarioId());
        return new RotationResult(stored.getUsuarioId(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * REQUIRES_NEW a proposito: rotate() la llama justo antes de lanzar
     * BadCredentialsException, y rotate() tambien es @Transactional. Sin una
     * sub-transaccion propia que ya haya hecho commit, el rollback-on-
     * exception de la transaccion de rotate() deshacia esta revocacion en
     * silencio -- la respuesta de seguridad ante un token reutilizado/robado
     * nunca llegaba a Postgres. (Requiere llamarse via el campo "self", ver
     * su Javadoc: REQUIRES_NEW no hace nada en una self-invocation.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(UUID usuarioId) {
        refreshTokenRepository.findAllByUsuarioIdAndRevokedFalse(usuarioId)
                .forEach(token -> token.setRevoked(true));
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
