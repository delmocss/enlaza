package com.enlaza.factura;

import com.enlaza.cliente.Cliente;
import com.enlaza.cliente.ClienteRepository;
import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.common.hash.Sha256;
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

        List<LineaFactura> lineas = request.lineas().stream().map(this::calcularLinea).toList();
        BigDecimal baseImponible = sumar(lineas, LineaFactura::getImporteBase);
        BigDecimal totalIva = sumar(lineas, LineaFactura::getImporteIva);
        BigDecimal totalIrpf = sumar(lineas, LineaFactura::getImporteIrpf);
        BigDecimal total = sumar(lineas, LineaFactura::getImporteTotal);

        int numero = serie.getUltimoNumero() + 1;
        Instant fechaEmision = Instant.now();
        String hashAnterior = serie.getUltimoHash();
        String hashActual = calcularHash(serieId, numero, fechaEmision, cliente.getId(),
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
        BigDecimal importeBase = request.cantidad().multiply(request.precioUnitario()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal importeIva = importeBase.multiply(request.porcentajeIva())
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal importeIrpf = importeBase.multiply(request.porcentajeIrpf())
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal importeTotal = importeBase.add(importeIva).subtract(importeIrpf);

        return LineaFactura.builder()
                .descripcion(request.descripcion())
                .cantidad(request.cantidad())
                .precioUnitario(request.precioUnitario())
                .porcentajeIva(request.porcentajeIva())
                .porcentajeIrpf(request.porcentajeIrpf())
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
                .findByFacturaIdIn(facturas.stream().map(Factura::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(LineaFactura::getFacturaId,
                        Collectors.mapping(LineaFacturaResponse::of, Collectors.toList())));

        return facturas.stream()
                .map(f -> FacturaResponse.of(f, lineasPorFactura.getOrDefault(f.getId(), List.of())))
                .toList();
    }

    /**
     * Incluye baseImponible/totalIva/totalIrpf y el contenido de cada linea
     * (no solo el total): dos facturas con el mismo total pero distinta
     * composicion fiscal o distintas lineas deben producir hashes distintos,
     * o alterar una linea directamente en la base de datos sin tocar el
     * total pasaria desapercibido para la cadena de integridad.
     */
    private String calcularHash(UUID serieId, int numero, Instant fechaEmision, UUID clienteId,
                                 BigDecimal baseImponible, BigDecimal totalIva, BigDecimal totalIrpf, BigDecimal total,
                                 List<LineaFactura> lineas, String hashAnterior) {
        String lineasContenido = lineas.stream()
                .map(l -> String.join(":",
                        l.getDescripcion(),
                        l.getCantidad().toPlainString(),
                        l.getPrecioUnitario().toPlainString(),
                        l.getPorcentajeIva().toPlainString(),
                        l.getPorcentajeIrpf().toPlainString(),
                        l.getImporteTotal().toPlainString()))
                .collect(Collectors.joining(";"));

        String contenido = String.join("|",
                serieId.toString(),
                String.valueOf(numero),
                fechaEmision.toString(),
                clienteId.toString(),
                baseImponible.toPlainString(),
                totalIva.toPlainString(),
                totalIrpf.toPlainString(),
                total.toPlainString(),
                lineasContenido,
                hashAnterior);
        return Sha256.hex(contenido);
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
        return lineaFacturaRepository.findByFacturaId(facturaId).stream().map(LineaFacturaResponse::of).toList();
    }
}
