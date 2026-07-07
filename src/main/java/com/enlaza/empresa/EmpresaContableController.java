package com.enlaza.empresa;

import com.enlaza.empresa.dto.ContableAsignadoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Asignacion de usuarios CONTABLE a una Empresa: solo ADMIN. Esto es lo que
 * despues consulta EmpresaAccessService para decidir si un CONTABLE puede
 * operar sobre Cliente/SerieFacturacion de esta empresa.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/contables")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmpresaContableController {

    private final UsuarioEmpresaService usuarioEmpresaService;

    @GetMapping
    public ResponseEntity<List<ContableAsignadoResponse>> listar(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(usuarioEmpresaService.listarAsignados(empresaId));
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<Void> asignar(@PathVariable UUID empresaId, @PathVariable UUID usuarioId) {
        usuarioEmpresaService.asignar(empresaId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> desasignar(@PathVariable UUID empresaId, @PathVariable UUID usuarioId) {
        usuarioEmpresaService.desasignar(empresaId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
