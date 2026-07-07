package com.enlaza.empresa;

import com.enlaza.security.EnlazaUserPrincipal;
import com.enlaza.usuario.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Autorizacion por datos, no solo por rol: un ADMIN puede operar sobre
 * cualquier Empresa; un CONTABLE solo sobre las que tiene asignadas via
 * UsuarioEmpresa. La usan Cliente y SerieFacturacion, que estan scoped a
 * una Empresa.
 */
@Service
@RequiredArgsConstructor
public class EmpresaAccessService {

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    public void verificarAcceso(EnlazaUserPrincipal principal, UUID empresaId) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        if (principal.getRole() == Role.CONTABLE
                && usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(principal.getId(), empresaId)) {
            return;
        }
        throw new AccessDeniedException("No tienes acceso a esta empresa");
    }
}
