package com.enlaza.serie;

import com.enlaza.security.EnlazaUserPrincipal;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
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
 * Scoped a una Empresa, mismo control de acceso por datos que Cliente. No
 * hay endpoint de edicion de codigo ni borrado fisico: una serie es un
 * registro de numeracion, solo se activa/desactiva.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/series")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTABLE')")
public class SerieFacturacionController {

    private final SerieFacturacionService serieFacturacionService;

    @PostMapping
    public ResponseEntity<SerieFacturacionResponse> crear(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @Valid @RequestBody SerieFacturacionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serieFacturacionService.crear(principal, empresaId, request));
    }

    @GetMapping
    public ResponseEntity<List<SerieFacturacionResponse>> listar(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId
    ) {
        return ResponseEntity.ok(serieFacturacionService.listar(principal, empresaId));
    }

    @GetMapping("/{serieId}")
    public ResponseEntity<SerieFacturacionResponse> obtener(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID serieId
    ) {
        return ResponseEntity.ok(serieFacturacionService.obtener(principal, empresaId, serieId));
    }

    @PatchMapping("/{serieId}/activar")
    public ResponseEntity<SerieFacturacionResponse> activar(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID serieId
    ) {
        return ResponseEntity.ok(serieFacturacionService.activar(principal, empresaId, serieId));
    }

    @PatchMapping("/{serieId}/desactivar")
    public ResponseEntity<SerieFacturacionResponse> desactivar(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID serieId
    ) {
        return ResponseEntity.ok(serieFacturacionService.desactivar(principal, empresaId, serieId));
    }
}
