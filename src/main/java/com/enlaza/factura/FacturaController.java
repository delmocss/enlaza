package com.enlaza.factura;

import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.factura.dto.SerieVerificacionResponse;
import com.enlaza.factura.pdf.FacturaPdfService;
import com.enlaza.security.EnlazaUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Scoped a una Empresa (y dentro, a una Serie). Mismo patron de
 * autorizacion declarativa que Cliente/SerieFacturacion (ver
 * EmpresaAccessService). A diferencia de esos dos, aqui si necesitamos el
 * principal autenticado -- no solo para autorizar (eso ya lo hizo
 * @PreAuthorize antes de que el metodo se ejecute), sino como dato de
 * negocio real: quien creo la factura.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/series/{serieId}/facturas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTABLE')")
public class FacturaController {

    private final FacturaService facturaService;
    private final SerieVerificacionService serieVerificacionService;
    private final FacturaPdfService facturaPdfService;

    @PostMapping
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<FacturaResponse> crear(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID serieId,
            @Valid @RequestBody FacturaRequest request
    ) {
        FacturaResponse response = facturaService.crear(empresaId, serieId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<List<FacturaResponse>> listar(@PathVariable UUID empresaId, @PathVariable UUID serieId) {
        return ResponseEntity.ok(facturaService.listar(empresaId, serieId));
    }

    /**
     * Mapeada ANTES de /{facturaId}: Spring da prioridad a un segmento
     * literal ("/verificacion") sobre una plantilla de variable de ruta
     * cuando ambas podrian coincidir, asi que no colisiona con obtener().
     */
    @GetMapping("/verificacion")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<SerieVerificacionResponse> verificar(@PathVariable UUID empresaId, @PathVariable UUID serieId) {
        return ResponseEntity.ok(serieVerificacionService.verificar(empresaId, serieId));
    }

    @GetMapping("/{facturaId}")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<FacturaResponse> obtener(
            @PathVariable UUID empresaId, @PathVariable UUID serieId, @PathVariable UUID facturaId) {
        return ResponseEntity.ok(facturaService.obtener(empresaId, serieId, facturaId));
    }

    @GetMapping("/{facturaId}/pdf")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<byte[]> pdf(
            @PathVariable UUID empresaId, @PathVariable UUID serieId, @PathVariable UUID facturaId) {
        return facturaPdfService.generar(empresaId, serieId, facturaId).comoDescarga();
    }

    @PatchMapping("/{facturaId}/anular")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<FacturaResponse> anular(
            @PathVariable UUID empresaId, @PathVariable UUID serieId, @PathVariable UUID facturaId) {
        return ResponseEntity.ok(facturaService.anular(empresaId, serieId, facturaId));
    }
}
