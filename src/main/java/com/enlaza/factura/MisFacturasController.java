package com.enlaza.factura;

import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.security.EnlazaUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Portal de solo lectura para el rol CLIENTE: sus propias facturas, sin
 * pasar por empresa/serie en la URL -- resuelto por Usuario.clienteId del
 * propio principal autenticado, no por asignacion.
 */
@RestController
@RequestMapping("/api/mis-facturas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class MisFacturasController {

    private final FacturaService facturaService;

    @GetMapping
    public ResponseEntity<List<FacturaResponse>> listar(@AuthenticationPrincipal EnlazaUserPrincipal principal) {
        return ResponseEntity.ok(facturaService.listarPorUsuarioCliente(principal.getId()));
    }
}
