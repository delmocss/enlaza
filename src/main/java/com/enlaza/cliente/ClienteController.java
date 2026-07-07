package com.enlaza.cliente;

import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.security.EnlazaUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Scoped a una Empresa. ADMIN entra siempre; CONTABLE solo si tiene la
 * empresa asignada -- lo verifica EmpresaAccessService dentro de cada
 * metodo de ClienteService, no aqui (el rol pasa la puerta, los datos
 * deciden el acceso real).
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTABLE')")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @Valid @RequestBody ClienteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(principal, empresaId, request));
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId
    ) {
        return ResponseEntity.ok(clienteService.listar(principal, empresaId));
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<ClienteResponse> obtener(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID clienteId
    ) {
        return ResponseEntity.ok(clienteService.obtener(principal, empresaId, clienteId));
    }

    @PutMapping("/{clienteId}")
    public ResponseEntity<ClienteResponse> actualizar(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID clienteId,
            @Valid @RequestBody ClienteRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizar(principal, empresaId, clienteId, request));
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal EnlazaUserPrincipal principal,
            @PathVariable UUID empresaId,
            @PathVariable UUID clienteId
    ) {
        clienteService.eliminar(principal, empresaId, clienteId);
        return ResponseEntity.noContent().build();
    }
}
