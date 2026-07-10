package com.enlaza.factura.pdf;

import com.enlaza.cliente.Cliente;
import com.enlaza.cliente.ClienteRepository;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.Empresa;
import com.enlaza.empresa.EmpresaService;
import com.enlaza.factura.FacturaService;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.serie.SerieFacturacion;
import com.enlaza.serie.SerieFacturacionRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Genera el PDF de una Factura: Thymeleaf construye el HTML del documento
 * (nunca sirve la interfaz de la aplicacion, ver docs/01-modelo-datos.md D1)
 * y OpenHTMLtoPDF lo convierte a PDF. Reutiliza las comprobaciones de acceso
 * que ya existen en FacturaService (empresa-scoped para ADMIN/CONTABLE, por
 * Usuario.clienteId para CLIENTE) en vez de reimplementarlas aqui.
 */
@Service
@RequiredArgsConstructor
public class FacturaPdfService {

    // Zona fija (no systemDefault()): la fecha impresa en la factura no
    // puede depender de en que servidor/contenedor corra el JVM -- con
    // systemDefault() el mismo Instant se mostraria distinto en local que
    // en un contenedor Docker con TZ=UTC, aunque el Instant almacenado y el
    // hash no cambien.
    private static final ZoneId ZONA_FACTURACION = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA_FACTURACION);

    private final FacturaService facturaService;
    private final SerieFacturacionRepository serieFacturacionRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaService empresaService;
    private final ITemplateEngine templateEngine;

    public FacturaPdf generar(UUID empresaId, UUID serieId, UUID facturaId) {
        FacturaResponse factura = facturaService.obtener(empresaId, serieId, facturaId);
        SerieFacturacion serie = buscarSerie(serieId);
        Empresa empresa = empresaService.buscarPorId(empresaId);
        return renderizar(factura, empresa, serie);
    }

    /**
     * Portal CLIENTE: la autorizacion ya la resolvio
     * FacturaService.obtenerParaUsuarioCliente (por Usuario.clienteId, no
     * por asignacion de Empresa), asi que aqui solo falta resolver la
     * Empresa/Serie para mostrarlas en el documento.
     */
    public FacturaPdf generarParaUsuarioCliente(UUID facturaId, UUID usuarioId) {
        FacturaResponse factura = facturaService.obtenerParaUsuarioCliente(facturaId, usuarioId);
        SerieFacturacion serie = buscarSerie(factura.serieId());
        Empresa empresa = empresaService.buscarPorId(serie.getEmpresaId());
        return renderizar(factura, empresa, serie);
    }

    private FacturaPdf renderizar(FacturaResponse factura, Empresa empresa, SerieFacturacion serie) {
        Cliente cliente = clienteRepository.findById(factura.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + factura.clienteId()));

        Context context = new Context();
        context.setVariable("factura", factura);
        context.setVariable("empresa", empresa);
        context.setVariable("cliente", cliente);
        context.setVariable("serie", serie);
        context.setVariable("fechaEmisionFormateada", FORMATO_FECHA.format(factura.fechaEmision()));

        String html = templateEngine.process("factura", context);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(salida);
        try {
            builder.run();
        } catch (IOException e) {
            throw new IllegalStateException("Error generando el PDF de la factura " + factura.id(), e);
        }

        String nombreArchivo = "factura-%s-%d.pdf".formatted(serie.getCodigo(), factura.numero());
        return new FacturaPdf(salida.toByteArray(), nombreArchivo);
    }

    private SerieFacturacion buscarSerie(UUID serieId) {
        return serieFacturacionRepository.findById(serieId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie de facturacion no encontrada: " + serieId));
    }
}
