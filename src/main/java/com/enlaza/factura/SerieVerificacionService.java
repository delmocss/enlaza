package com.enlaza.factura;

import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.factura.dto.SerieVerificacionResponse;
import com.enlaza.serie.SerieFacturacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SerieVerificacionService {

    private final SerieFacturacionRepository serieFacturacionRepository;
    private final FacturaRepository facturaRepository;
    private final LineaFacturaRepository lineaFacturaRepository;

    /**
     * Recorre la cadena de facturas de una serie en orden de numero y
     * recalcula cada hash de forma independiente con FacturaHashCalculator,
     * sin fiarse de ningun valor guardado -- exactamente la misma
     * comprobacion que haria un tercero para confiar (o no) en la serie.
     * Se detiene en la primera rotura encontrada: ya demuestra el objetivo
     * (detectar manipulacion) sin necesidad de listar todas las facturas
     * afectadas.
     *
     * Limitacion conocida: esta formula (y su normalizacion de escala de
     * BigDecimal / precision de Instant) solo es valida hacia adelante.
     * Una Factura persistida con una version anterior de calcularLinea()
     * (antes de esa normalizacion) recalcularia un hash distinto al
     * guardado, no porque haya sido manipulada sino porque la formula
     * cambio -- no existe (ni es seguro añadir) una migracion que
     * recalcule hashes historicos, porque no habria forma de distinguir
     * "cambio de formula" de "manipulacion real" en datos ya persistidos.
     * En un entorno con Factura anteriores a este fix, hay que partir de
     * una base de datos limpia (reseed) para que la verificacion sea
     * fiable.
     */
    public SerieVerificacionResponse verificar(UUID empresaId, UUID serieId) {
        serieFacturacionRepository.findByIdAndEmpresaId(serieId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie de facturacion no encontrada: " + serieId));

        List<Factura> facturas = facturaRepository.findBySerieIdOrderByNumeroAsc(serieId);
        Map<UUID, List<LineaFactura>> lineasPorFactura = lineaFacturaRepository
                .findByFacturaIdInOrderByFacturaIdAscOrdenAsc(facturas.stream().map(Factura::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(LineaFactura::getFacturaId));
        String hashEsperado = FacturaHashCalculator.genesis();

        for (Factura factura : facturas) {
            if (!factura.getHashAnterior().equals(hashEsperado)) {
                return SerieVerificacionResponse.rota(serieId, facturas.size(), factura.getId(), factura.getNumero(),
                        "hashAnterior no coincide con el hash de la factura previa en la cadena");
            }

            List<LineaFactura> lineas = lineasPorFactura.getOrDefault(factura.getId(), List.of());
            String hashRecalculado = FacturaHashCalculator.calcular(
                    factura.getSerieId(), factura.getNumero(), factura.getFechaEmision(), factura.getClienteId(),
                    factura.getBaseImponible(), factura.getTotalIva(), factura.getTotalIrpf(), factura.getTotal(),
                    lineas, factura.getHashAnterior());
            if (!hashRecalculado.equals(factura.getHashActual())) {
                return SerieVerificacionResponse.rota(serieId, facturas.size(), factura.getId(), factura.getNumero(),
                        "el contenido de la factura no coincide con su hash guardado");
            }

            hashEsperado = factura.getHashActual();
        }

        return SerieVerificacionResponse.integra(serieId, facturas.size());
    }
}
