package com.enlaza.empresa;

import com.enlaza.security.EnlazaUserPrincipal;
import com.enlaza.usuario.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Autorizacion por datos, no solo por rol: un ADMIN puede operar sobre
 * cualquier Empresa; un CONTABLE solo sobre las que tiene asignadas via
 * UsuarioEmpresa. La usan Cliente y SerieFacturacion, que estan scoped a
 * una Empresa.
 *
 * Se invoca desde @PreAuthorize en cada metodo de controlador (no con una
 * llamada manual al principio de cada metodo de servicio): asi el chequeo
 * lo aplica Spring Security ANTES de que el metodo se ejecute, igual que el
 * resto de la autorizacion del proyecto, y un metodo nuevo que olvide la
 * anotacion cae al @PreAuthorize de clase (bloquea CLIENTE) en vez de
 * quedar completamente sin proteger.
 */
@Service
@RequiredArgsConstructor
public class EmpresaAccessService {

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    public boolean tieneAcceso(Authentication authentication, UUID empresaId) {
        EnlazaUserPrincipal principal = (EnlazaUserPrincipal) authentication.getPrincipal();
        if (principal.getRole() == Role.ADMIN) {
            return true;
        }
        return principal.getRole() == Role.CONTABLE
                && usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(principal.getId(), empresaId);
    }
}
