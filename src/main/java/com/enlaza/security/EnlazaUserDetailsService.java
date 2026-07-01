package com.enlaza.security;

import com.enlaza.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Solo la usa el login (autenticacion por email+password via
 * AuthenticationManager). Las peticiones ya autenticadas por JWT no pasan por
 * aqui -- ver JwtAuthenticationFilter, que recarga al usuario por id.
 */
@Service
@RequiredArgsConstructor
public class EnlazaUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(EnlazaUserPrincipal::of)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }
}
