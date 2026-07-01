package com.enlaza.security.seed;

import com.enlaza.usuario.Role;
import com.enlaza.usuario.Usuario;
import com.enlaza.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario semilla por rol (ADMIN, CONTABLE, CLIENTE) si todavia no
 * existen, para que cualquier despliegue (local o el de portfolio) se pueda
 * probar con los tres roles sin preparar datos a mano. Idempotente: no hace
 * nada si el email ya existe. El CONTABLE y el CLIENTE quedan sin empresa
 * asignada / sin cliente vinculado hasta la Fase 4, cuando existan esas
 * tablas -- esta clase expone los Usuario creados para que el seeder de la
 * Fase 4 no tenga que volver a buscarlos por email.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    @Override
    public void run(String... args) {
        seedIfMissing(seedProperties.getAdminEmail(), seedProperties.getAdminPassword(), "Administrador Demo", Role.ADMIN);
        seedIfMissing(seedProperties.getContableEmail(), seedProperties.getContablePassword(), "Contable Demo", Role.CONTABLE);
        seedIfMissing(seedProperties.getClienteEmail(), seedProperties.getClientePassword(), "Cliente Demo", Role.CLIENTE);

        log.warn("Usuarios semilla listos. Sus credenciales son PUBLICAS a proposito (portfolio de demo) "
                + "-- ver .env.example. No uses esta configuracion para una instancia con datos reales "
                + "sin cambiar ADMIN_PASSWORD, DEMO_*_PASSWORD y JWT_SECRET.");
    }

    private Usuario seedIfMissing(String email, String rawPassword, String nombre, Role role) {
        return usuarioRepository.findByEmail(email)
                .orElseGet(() -> {
                    Usuario usuario = Usuario.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode(rawPassword))
                            .nombre(nombre)
                            .role(role)
                            .enabled(true)
                            .build();
                    Usuario saved = usuarioRepository.save(usuario);
                    log.info("Usuario semilla creado: {} ({})", email, role);
                    return saved;
                });
    }
}
