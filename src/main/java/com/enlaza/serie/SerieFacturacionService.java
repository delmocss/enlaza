package com.enlaza.serie;

import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.EmpresaAccessService;
import com.enlaza.empresa.EmpresaService;
import com.enlaza.security.EnlazaUserPrincipal;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    private final EmpresaAccessService empresaAccessService;

    @Transactional
    public SerieFacturacionResponse crear(EnlazaUserPrincipal principal, UUID empresaId, SerieFacturacionRequest request) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        empresaService.buscarPorId(empresaId);

        if (serieFacturacionRepository.existsByEmpresaIdAndCodigo(empresaId, request.codigo())) {
            throw new BusinessRuleException("Ya existe una serie con codigo " + request.codigo() + " en esta empresa");
        }

        SerieFacturacion serie = SerieFacturacion.builder()
                .empresaId(empresaId)
                .codigo(request.codigo())
                .ultimoNumero(0)
                .ultimoHash(hashGenesis())
                .activa(true)
                .build();

        return SerieFacturacionResponse.of(serieFacturacionRepository.save(serie));
    }

    public List<SerieFacturacionResponse> listar(EnlazaUserPrincipal principal, UUID empresaId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        return serieFacturacionRepository.findByEmpresaId(empresaId).stream()
                .map(SerieFacturacionResponse::of)
                .toList();
    }

    public SerieFacturacionResponse obtener(EnlazaUserPrincipal principal, UUID empresaId, UUID serieId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        return SerieFacturacionResponse.of(buscarPorIdYEmpresa(serieId, empresaId));
    }

    @Transactional
    public SerieFacturacionResponse activar(EnlazaUserPrincipal principal, UUID empresaId, UUID serieId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        SerieFacturacion serie = buscarPorIdYEmpresa(serieId, empresaId);
        serie.setActiva(true);
        return SerieFacturacionResponse.of(serie);
    }

    @Transactional
    public SerieFacturacionResponse desactivar(EnlazaUserPrincipal principal, UUID empresaId, UUID serieId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        SerieFacturacion serie = buscarPorIdYEmpresa(serieId, empresaId);
        serie.setActiva(false);
        return SerieFacturacionResponse.of(serie);
    }

    private SerieFacturacion buscarPorIdYEmpresa(UUID serieId, UUID empresaId) {
        return serieFacturacionRepository.findByIdAndEmpresaId(serieId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie de facturacion no encontrada: " + serieId));
    }

    private static String hashGenesis() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(GENESIS_SEED.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
