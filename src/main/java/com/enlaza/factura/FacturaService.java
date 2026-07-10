package com.enlaza.factura;

import com.enlaza.cliente.Cliente;
import com.enlaza.cliente.ClienteRepository;
import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.Empresa;
import com.enlaza.empresa.EmpresaService;
import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.factura.dto.LineaFacturaRequest;
import com.enlaza.factura.dto.LineaFacturaResponse;
import com.enlaza.serie.SerieFacturacion;
import com.enlaza.serie.SerieFacturacionRepository;
import com.enlaza.usuario.Usuario;
import com.enlaza.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final FacturaRepository facturaRepository;
    private final LineaFacturaRepository lineaFacturaRepository;
    private final SerieFacturacionRepository serieFacturacionRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaService empresaService;
    private final UsuarioRepository usuarioRepository;

    /**
     * El lock PESSIMISTIC_WRITE sobre la fila de SerieFacturacion se
     * mantiene durante toda la transaccion: una segunda peticion concurrente
     * de creacion en la MISMA serie espera aqui hasta que esta termine
     * (commit o rollback), y entonces lee el ultimoNumero/ultimoHash ya
     * actualizado -- sin eso, dos peticiones podrian leer el mismo
     * ultimoNumero antes de que ninguna escriba y emitir el mismo numero
     * dos veces (D5 del modelo de datos).
     */
    @Transactional
    public FacturaResponse crear(UUID empresaId, UUID serieId, FacturaRequest request, UUID creadoPor) {
        Empresa empresa = empresaService.buscarPorId(empresaId);
        if (!empresa.isActiva()) {
            throw new BusinessRuleException("No se pueden emitir facturas en una empresa desactivada");
        }

        // Validar el Cliente ANTES de tomar el lock sobre la serie: si la
        // peticion va a fallar de todos modos, mejor no bloquear a otras
        // peticiones concurrentes que si podrian numerar correctamente.
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(request.clienteId(), empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.clienteId()));

        SerieFacturacion serie = serieFacturacionRepository.findByIdAndEmpresaIdForUpdate(serieId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie de facturacion no encontrada: " + serieId));
        if (!serie.isActiva()) {
            throw new BusinessRuleException("No se pueden emitir facturas en una serie desactivada");
        }

        List<LineaFacturaRequest> lineasRequest = request.lineas();
        List<LineaFactura> lineas = java.util.stream.IntStream.range(0, lineasRequest.size())
                .mapToObj(i -> {
                    LineaFactura linea = calcularLinea(lineasRequest.get(i));
                    linea.setOrden(i);
                    return linea;
                })
                .toList();
        BigDecimal baseImponible = sumar(lineas, LineaFactura::getImporteBase);
        BigDecimal totalIva = sumar(lineas, LineaFactura::getImporteIva);
        BigDecimal totalIrpf = sumar(lineas, LineaFactura::getImporteIrpf);
        BigDecimal total = sumar(lineas, LineaFactura::getImporteTotal);

        int numero = serie.getUltimoNumero() + 1;
        // Truncado a milisegundos: la columna fecha_emision es TIMESTAMP
        // (precision de microsegundos en Postgres). Sin truncar, un
        // Instant.now() con nanosegundos no nulos se guarda con perdida de
        // precision -- al releerlo para verificar la cadena, su toString()
        // cambia respecto al valor usado para calcular hashActual, y la
        // verificacion (Fase 6) marcaria una factura intacta como
        // manipulada. Truncar ANTES de hashear garantiza que el valor
        // persistido y el usado en el hash sean siempre el mismo.
        Instant fechaEmision = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        String hashAnterior = serie.getUltimoHash();
        String hashActual = FacturaHashCalculator.calcular(serieId, numero, fechaEmision, cliente.getId(),
                baseImponible, totalIva, totalIrpf, total, lineas, hashAnterior);

        Factura factura = Factura.builder()
                .serieId(serieId)
                .numero(numero)
                .fechaEmision(fechaEmision)
                .clienteId(cliente.getId())
                .estado(EstadoFactura.EMITIDA)
                .hashAnterior(hashAnterior)
                .hashActual(hashActual)
                .baseImponible(baseImponible)
                .totalIva(totalIva)
                .totalIrpf(totalIrpf)
                .total(total)
                .creadoPor(creadoPor)
                .build();
        Factura facturaGuardada = facturaRepository.save(factura);

        lineas.forEach(linea -> linea.setFacturaId(facturaGuardada.getId()));
        List<LineaFactura> lineasGuardadas = lineaFacturaRepository.saveAll(lineas);

        serie.setUltimoNumero(numero);
        serie.setUltimoHash(hashActual);

        return FacturaResponse.of(facturaGuardada, lineasGuardadas.stream().map(LineaFacturaResponse::of).toList());
    }

    public List<FacturaResponse> listar(UUID empresaId, UUID serieId) {
        verificarSerieDeEmpresa(empresaId, serieId);
        return construirRespuestas(facturaRepository.findBySerieId(serieId));
    }

    public FacturaResponse obtener(UUID empresaId, UUID serieId, UUID facturaId) {
        verificarSerieDeEmpresa(empresaId, serieId);
        Factura factura = buscarPorIdYSerie(facturaId, serieId);
        return FacturaResponse.of(factura, lineasDe(facturaId));
    }

    /**
     * Portal del CLIENTE: sus propias facturas, resueltas via
     * Usuario.clienteId, no via EmpresaAccessService -- es acceso a datos
     * propios, no una asignacion a una Empresa.
     */
    public List<FacturaResponse> listarPorUsuarioCliente(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
        if (usuario.getClienteId() == null) {
            return List.of();
        }

        return construirRespuestas(facturaRepository.findByClienteIdOrderByFechaEmisionDesc(usuario.getClienteId()));
    }

    /**
     * Igual que listarPorUsuarioCliente pero para una unica factura (usada
     * por la exportacion a PDF del portal CLIENTE). Un facturaId que existe
     * pero pertenece a otro cliente se trata igual que uno inexistente
     * (ResourceNotFoundException -> 404), para no filtrar por el codigo de
     * error si una factura ajena existe.
     */
    public FacturaResponse obtenerParaUsuarioCliente(UUID facturaId, UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
        Factura factura = facturaRepository.findById(facturaId)
                .filter(f -> f.getClienteId().equals(usuario.getClienteId()))
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + facturaId));
        return FacturaResponse.of(factura, lineasDe(facturaId));
    }

    @Transactional
    public FacturaResponse anular(UUID empresaId, UUID serieId, UUID facturaId) {
        verificarSerieDeEmpresa(empresaId, serieId);
        Factura factura = buscarPorIdYSerie(facturaId, serieId);
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new BusinessRuleException("La factura ya esta anulada");
        }

        factura.setEstado(EstadoFactura.ANULADA);
        return FacturaResponse.of(factura, lineasDe(facturaId));
    }

    private LineaFactura calcularLinea(LineaFacturaRequest request) {
        // cantidad/precioUnitario/porcentajeIva/porcentajeIrpf se normalizan
        // a la misma escala (2 decimales) que sus columnas NUMERIC antes de
        // usarse en el hash: si se guardara la escala cruda del request (p.
        // ej. BigDecimal.ONE -> "1"), Postgres persiste igualmente con
        // escala 2 y una relectura para verificar devolveria "1.00" ->
        // toPlainString() distinto -> la verificacion (Fase 6) marcaria una
        // factura intacta como manipulada.
        // Este setScale(2) es un simple relleno de ceros, NUNCA un redondeo
        // real que altere el valor: LineaFacturaRequest ya exige
        // @Digits(fraction = 2), asi que ningun valor con mas de 2
        // decimales llega hasta aqui (evita una regresion de doble
        // redondeo si se calculara importeBase a partir de estos valores
        // ya truncados en vez de los del request).
        BigDecimal cantidad = request.cantidad().setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioUnitario = request.precioUnitario().setScale(2, RoundingMode.HALF_UP);
        BigDecimal porcentajeIva = request.porcentajeIva().setScale(2, RoundingMode.HALF_UP);
        BigDecimal porcentajeIrpf = request.porcentajeIrpf().setScale(2, RoundingMode.HALF_UP);

        BigDecimal importeBase = cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
        BigDecimal importeIva = importeBase.multiply(porcentajeIva)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal importeIrpf = importeBase.multiply(porcentajeIrpf)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal importeTotal = importeBase.add(importeIva).subtract(importeIrpf);

        return LineaFactura.builder()
                .descripcion(request.descripcion())
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .porcentajeIva(porcentajeIva)
                .porcentajeIrpf(porcentajeIrpf)
                .importeBase(importeBase)
                .importeIva(importeIva)
                .importeIrpf(importeIrpf)
                .importeTotal(importeTotal)
                .build();
    }

    private BigDecimal sumar(List<LineaFactura> lineas, java.util.function.Function<LineaFactura, BigDecimal> campo) {
        return lineas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<FacturaResponse> construirRespuestas(List<Factura> facturas) {
        Map<UUID, List<LineaFacturaResponse>> lineasPorFactura = lineaFacturaRepository
                .findByFacturaIdInOrderByFacturaIdAscOrdenAsc(facturas.stream().map(Factura::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(LineaFactura::getFacturaId,
                        Collectors.mapping(LineaFacturaResponse::of, Collectors.toList())));

        return facturas.stream()
                .map(f -> FacturaResponse.of(f, lineasPorFactura.getOrDefault(f.getId(), List.of())))
                .toList();
    }

    private void verificarSerieDeEmpresa(UUID empresaId, UUID serieId) {
        serieFacturacionRepository.findByIdAndEmpresaId(serieId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie de facturacion no encontrada: " + serieId));
    }

    private Factura buscarPorIdYSerie(UUID facturaId, UUID serieId) {
        return facturaRepository.findByIdAndSerieId(facturaId, serieId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + facturaId));
    }

    private List<LineaFacturaResponse> lineasDe(UUID facturaId) {
        return lineaFacturaRepository.findByFacturaIdOrderByOrdenAsc(facturaId).stream()
                .map(LineaFacturaResponse::of).toList();
    }
}
