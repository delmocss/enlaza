package com.enlaza.security.seed;

import com.enlaza.cliente.Cliente;
import com.enlaza.cliente.ClienteRepository;
import com.enlaza.empresa.Empresa;
import com.enlaza.empresa.EmpresaRepository;
import com.enlaza.empresa.UsuarioEmpresa;
import com.enlaza.empresa.UsuarioEmpresaRepository;
import com.enlaza.factura.FacturaService;
import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.LineaFacturaRequest;
import com.enlaza.serie.SerieFacturacionService;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import com.enlaza.usuario.Role;
import com.enlaza.usuario.Usuario;
import com.enlaza.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Crea un usuario semilla por rol (ADMIN, CONTABLE, CLIENTE) si todavia no
 * existen, para que cualquier despliegue (local o el de portfolio) se pueda
 * probar con los tres roles sin preparar datos a mano. Tambien siembra una
 * Empresa y un Cliente demo (Fase 4) y los vincula: el CONTABLE queda
 * asignado a la Empresa via UsuarioEmpresa, y el CLIENTE queda enlazado al
 * Cliente via Usuario.clienteId -- para que el sistema sembrado sea
 * funcional de punta a punta con los tres roles. Todo idempotente: no hace
 * nada si el dato ya existe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final String EMPRESA_DEMO_NIF = "B00000000";
    private static final String CLIENTE_DEMO_NIF = "12345678Z";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final SerieFacturacionService serieFacturacionService;
    private final FacturaService facturaService;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    @Override
    @Transactional
    public void run(String... args) {
        seedIfMissing(seedProperties.getAdminEmail(), seedProperties.getAdminPassword(), "Administrador Demo", Role.ADMIN);
        Usuario contable = seedIfMissing(seedProperties.getContableEmail(), seedProperties.getContablePassword(), "Contable Demo", Role.CONTABLE);
        Usuario cliente = seedIfMissing(seedProperties.getClienteEmail(), seedProperties.getClientePassword(), "Cliente Demo", Role.CLIENTE);

        Empresa empresaDemo = seedEmpresaDemo();
        seedAsignacionContable(contable, empresaDemo);
        Cliente clienteDemo = seedClienteDemo(empresaDemo, cliente);
        seedFacturasDemo(empresaDemo, clienteDemo, contable);

        log.warn("Usuarios semilla listos. Sus credenciales son PUBLICAS a proposito (portfolio de demo) "
                + "-- ver .env.example. No uses esta configuracion para una instancia con datos reales "
                + "sin cambiar ADMIN_PASSWORD, DEMO_*_PASSWORD y JWT_SECRET.");
    }

    private Empresa seedEmpresaDemo() {
        return empresaRepository.findAll().stream()
                .filter(e -> e.getNif().equals(EMPRESA_DEMO_NIF))
                .findFirst()
                .orElseGet(() -> {
                    Empresa empresa = Empresa.builder()
                            .razonSocial("Empresa Demo S.L.")
                            .nif(EMPRESA_DEMO_NIF)
                            .activa(true)
                            .build();
                    Empresa saved = empresaRepository.save(empresa);
                    log.info("Empresa semilla creada: {} ({})", saved.getRazonSocial(), saved.getNif());
                    return saved;
                });
    }

    private void seedAsignacionContable(Usuario contable, Empresa empresa) {
        if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(contable.getId(), empresa.getId())) {
            return;
        }
        usuarioEmpresaRepository.save(UsuarioEmpresa.builder()
                .usuarioId(contable.getId())
                .empresaId(empresa.getId())
                .build());
        log.info("Contable semilla asignado a la empresa demo: {}", contable.getEmail());
    }

    private Cliente seedClienteDemo(Empresa empresa, Usuario usuarioCliente) {
        Cliente clienteDemo = clienteRepository.findByEmpresaId(empresa.getId()).stream()
                .filter(c -> c.getNif().equals(CLIENTE_DEMO_NIF))
                .findFirst()
                .orElseGet(() -> {
                    Cliente nuevo = Cliente.builder()
                            .empresaId(empresa.getId())
                            .nombre("Cliente Demo S.L.")
                            .nif(CLIENTE_DEMO_NIF)
                            .build();
                    Cliente saved = clienteRepository.save(nuevo);
                    log.info("Cliente semilla creado: {} ({})", saved.getNombre(), saved.getNif());
                    return saved;
                });

        if (usuarioCliente.getClienteId() == null) {
            usuarioCliente.setClienteId(clienteDemo.getId());
            log.info("Usuario CLIENTE semilla vinculado al cliente demo: {}", usuarioCliente.getEmail());
        }
        return clienteDemo;
    }

    /**
     * Siembra una serie "A" con tres facturas encadenadas para que la demo
     * no arranque vacia (listados, detalle con hashes, PDF y verificacion de
     * la cadena funcionan desde el primer minuto). Pasa por FacturaService,
     * asi la numeracion y los hashes son los reales, no datos insertados a
     * mano. Solo actua si la empresa demo aun no tiene ninguna serie.
     */
    private void seedFacturasDemo(Empresa empresa, Cliente cliente, Usuario contable) {
        if (!serieFacturacionService.listar(empresa.getId()).isEmpty()) {
            return;
        }
        SerieFacturacionResponse serie = serieFacturacionService.crear(empresa.getId(), new SerieFacturacionRequest("A"));
        facturaService.crear(empresa.getId(), serie.id(), new FacturaRequest(cliente.getId(), List.of(
                linea("Desarrollo de API REST", "40", "55.00", "21", "0"),
                linea("Despliegue y puesta en marcha", "4", "60.00", "21", "0"))), contable.getId());
        facturaService.crear(empresa.getId(), serie.id(), new FacturaRequest(cliente.getId(), List.of(
                linea("Mantenimiento mensual", "1", "300.00", "21", "15"))), contable.getId());
        facturaService.crear(empresa.getId(), serie.id(), new FacturaRequest(cliente.getId(), List.of(
                linea("Formacion al equipo (jornada)", "2", "450.00", "21", "0"),
                linea("Material didactico", "1", "80.00", "10", "0"))), contable.getId());
        log.info("Serie A y 3 facturas encadenadas sembradas en la empresa demo");
    }

    private static LineaFacturaRequest linea(String descripcion, String cantidad, String precio, String iva, String irpf) {
        return new LineaFacturaRequest(descripcion, new BigDecimal(cantidad), new BigDecimal(precio),
                new BigDecimal(iva), new BigDecimal(irpf));
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
