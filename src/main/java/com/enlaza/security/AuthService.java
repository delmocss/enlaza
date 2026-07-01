package com.enlaza.security;

import com.enlaza.security.dto.ChangePasswordRequest;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.security.dto.MeResponse;
import com.enlaza.usuario.Role;
import com.enlaza.usuario.Usuario;
import com.enlaza.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public LoginResponse login(LoginRequest request) {
        // El AuthenticationManager ya consulta al usuario (via
        // EnlazaUserDetailsService) para verificar la contrasena; reutilizamos
        // ese mismo principal para emitir los tokens en vez de volver a
        // consultarlo por email.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        EnlazaUserPrincipal principal = (EnlazaUserPrincipal) authentication.getPrincipal();

        log.info("Login correcto: {}", principal.getUsername());
        return issueTokens(principal.getId(), principal.getUsername(), principal.getRole());
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        Usuario usuario = usuarioRepository.findById(rotation.usuarioId())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        String accessToken = jwtService.generateAccessToken(usuario.getId(), usuario.getEmail(), usuario.getRole());
        return LoginResponse.of(accessToken, rotation.newRawToken(), jwtService.getAccessTokenExpirationSeconds());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public MeResponse me(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        return new MeResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getRole());
    }

    @Transactional
    public void changePassword(UUID usuarioId, ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("La contrasena actual no es correcta");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // Si la contrasena se cambio porque se sospecha que la cuenta estaba
        // comprometida, un refresh token robado no debe seguir funcionando
        // solo porque el access token todavia no ha expirado.
        refreshTokenService.revokeAllForUser(usuarioId);
        log.info("Contrasena actualizada y sesiones revocadas: {}", usuario.getEmail());
    }

    private LoginResponse issueTokens(UUID userId, String email, Role role) {
        String accessToken = jwtService.generateAccessToken(userId, email, role);
        String refreshToken = refreshTokenService.issue(userId);
        return LoginResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpirationSeconds());
    }
}
