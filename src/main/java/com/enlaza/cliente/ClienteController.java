package com.enlaza.cliente;

import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Scoped a una Empresa. El @PreAuthorize de clase es un cierre de emergencia
 * (bloquea CLIENTE) por si algun metodo futuro olvida el suyo; el chequeo
 * real -- ADMIN entra siempre, CONTABLE solo si tiene la empresa asignada --
 * lo hace @empresaAccessService.tieneAcceso en cada metodo, declarativamente,
 * antes de que el metodo se ejecute.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTABLE')")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<ClienteResponse> crear(@PathVariable UUID empresaId, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(empresaId, request));
    }

    @GetMapping
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<List<ClienteResponse>> listar(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(clienteService.listar(empresaId));
    }

    @GetMapping("/{clienteId}")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable UUID empresaId, @PathVariable UUID clienteId) {
        return ResponseEntity.ok(clienteService.obtener(empresaId, clienteId));
    }

    @PutMapping("/{clienteId}")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable UUID empresaId, @PathVariable UUID clienteId, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(empresaId, clienteId, request));
    }

    @DeleteMapping("/{clienteId}")
    @PreAuthorize("@empresaAccessService.tieneAcceso(authentication, #empresaId)")
    public ResponseEntity<Void> eliminar(@PathVariable UUID empresaId, @PathVariable UUID clienteId) {
        clienteService.eliminar(empresaId, clienteId);
        return ResponseEntity.noContent().build();
    }
}
