package com.enlaza.serie;

import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Scoped a una Empresa, mismo patron de autorizacion declarativa que
 * ClienteController (ver EmpresaAccessService). No hay endpoint de edicion
 * de codigo ni borrado fisico: una serie es un registro de numeracion,
 * solo se activa/desactiva.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/series")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTABLE')")
public class SerieFacturacionController {

    private final SerieFacturacionService serieFacturacionService;

    @PostMapping
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<SerieFacturacionResponse> crear(
            @PathVariable UUID empresaId, @Valid @RequestBody SerieFacturacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serieFacturacionService.crear(empresaId, request));
    }

    @GetMapping
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<List<SerieFacturacionResponse>> listar(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(serieFacturacionService.listar(empresaId));
    }

    @GetMapping("/{serieId}")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<SerieFacturacionResponse> obtener(@PathVariable UUID empresaId, @PathVariable UUID serieId) {
        return ResponseEntity.ok(serieFacturacionService.obtener(empresaId, serieId));
    }

    @PatchMapping("/{serieId}/activar")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<SerieFacturacionResponse> activar(@PathVariable UUID empresaId, @PathVariable UUID serieId) {
        return ResponseEntity.ok(serieFacturacionService.activar(empresaId, serieId));
    }

    @PatchMapping("/{serieId}/desactivar")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<SerieFacturacionResponse> desactivar(@PathVariable UUID empresaId, @PathVariable UUID serieId) {
        return ResponseEntity.ok(serieFacturacionService.desactivar(empresaId, serieId));
    }
}
