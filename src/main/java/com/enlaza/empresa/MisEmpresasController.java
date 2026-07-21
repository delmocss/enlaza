package com.enlaza.empresa;

import com.enlaza.empresa.dto.EmpresaResponse;
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
 * Para el selector de empresa del dashboard: un ADMIN ve todas las empresas,
 * un CONTABLE solo las suyas asignadas. GET /api/empresas es ADMIN-only, asi
 * que un CONTABLE no tiene otra forma de listar las propias.
 */
@RestController
@RequestMapping("/api/mis-empresas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTABLE')")
public class MisEmpresasController {

    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listar(@AuthenticationPrincipal EnlazaUserPrincipal principal) {
        return ResponseEntity.ok(empresaService.listarParaUsuario(principal.getId(), principal.getRole()));
    }
}
