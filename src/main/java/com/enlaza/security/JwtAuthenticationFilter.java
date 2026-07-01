package com.enlaza.security;

import com.enlaza.usuario.Usuario;
import com.enlaza.usuario.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Recarga al usuario desde BD en cada peticion (por id, extraido del token) en
 * lugar de confiar en el rol/estado que llevaba el JWT en el momento de
 * emitirse. Es una consulta extra por peticion, pero significa que
 * deshabilitar un usuario o cambiarle el rol surte efecto de inmediato, no
 * cuando caduque su access token.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final String[] publicEndpoints;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Evita firmar/consultar en BD para rutas publicas: un token viejo
        // adjunto por error a /api/auth/login no debe costar una consulta.
        String path = request.getServletPath();
        return Arrays.stream(publicEndpoints).anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        extractToken(request)
                .flatMap(this::authenticate)
                .ifPresent(authentication -> {
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    private Optional<UsernamePasswordAuthenticationToken> authenticate(String token) {
        try {
            UUID userId = jwtService.extractUserId(token);
            return usuarioRepository.findById(userId)
                    .filter(Usuario::isEnabled)
                    .map(EnlazaUserPrincipal::of)
                    .map(principal -> new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token invalido o expirado: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
