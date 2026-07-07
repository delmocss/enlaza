package com.enlaza.serie;

import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.common.hash.Sha256;
import com.enlaza.empresa.Empresa;
import com.enlaza.empresa.EmpresaService;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SerieFacturacionService {

    /**
     * Semilla de la cadena de hashes de una serie nueva: SHA-256 de una
     * cadena fija, no de datos de negocio (la primera factura real de la
     * serie enlazara contra este valor como su hashAnterior -- ver D6/D10
     * del modelo de datos).
     */
    private static final String GENESIS_SEED = "ENLAZA-GENESIS";

    private final SerieFacturacionRepository serieFacturacionRepository;
    private final EmpresaService empresaService;

    @Transactional
    public SerieFacturacionResponse crear(UUID empresaId, SerieFacturacionRequest request) {
        Empresa empresa = empresaService.buscarPorId(empresaId);
        if (!empresa.isActiva()) {
            throw new BusinessRuleException("No se pueden crear series en una empresa desactivada");
        }

        if (serieFacturacionRepository.existsByEmpresaIdAndCodigo(empresaId, request.codigo())) {
            throw new BusinessRuleException("Ya existe una serie con codigo " + request.codigo() + " en esta empresa");
        }

        SerieFacturacion serie = SerieFacturacion.builder()
                .empresaId(empresaId)
                .codigo(request.codigo())
                .ultimoNumero(0)
                .ultimoHash(Sha256.hex(GENESIS_SEED))
                .activa(true)
                .build();

        return SerieFacturacionResponse.of(serieFacturacionRepository.save(serie));
    }

    public List<SerieFacturacionResponse> listar(UUID empresaId) {
        return serieFacturacionRepository.findByEmpresaId(empresaId).stream()
                .map(SerieFacturacionResponse::of)
                .toList();
    }

    public SerieFacturacionResponse obtener(UUID empresaId, UUID serieId) {
        return SerieFacturacionResponse.of(buscarPorIdYEmpresa(serieId, empresaId));
    }

    @Transactional
    public SerieFacturacionResponse activar(UUID empresaId, UUID serieId) {
        SerieFacturacion serie = buscarPorIdYEmpresa(serieId, empresaId);
        serie.setActiva(true);
        return SerieFacturacionResponse.of(serie);
    }

    @Transactional
    public SerieFacturacionResponse desactivar(UUID empresaId, UUID serieId) {
        SerieFacturacion serie = buscarPorIdYEmpresa(serieId, empresaId);
        serie.setActiva(false);
        return SerieFacturacionResponse.of(serie);
    }

    private SerieFacturacion buscarPorIdYEmpresa(UUID serieId, UUID empresaId) {
        return serieFacturacionRepository.findByIdAndEmpresaId(serieId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie de facturacion no encontrada: " + serieId));
    }
}
