package com.enlaza.empresa;

import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestion de empresas: solo ADMIN. El acceso de CONTABLE se resuelve por
 * asignacion (UsuarioEmpresa), no aqui -- ver EmpresaAccessService.
 */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listar() {
        return ResponseEntity.ok(empresaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(empresaService.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.ok(empresaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<EmpresaResponse> activar(@PathVariable UUID id) {
        return ResponseEntity.ok(empresaService.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<EmpresaResponse> desactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(empresaService.desactivar(id));
    }
}
