package com.enlaza.empresa;

import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.dto.ContableAsignadoResponse;
import com.enlaza.usuario.Role;
import com.enlaza.usuario.Usuario;
import com.enlaza.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioEmpresaService {

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaService empresaService;

    @Transactional
    public void asignar(UUID empresaId, UUID usuarioId) {
        empresaService.buscarPorId(empresaId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
        if (usuario.getRole() != Role.CONTABLE) {
            throw new BusinessRuleException("Solo se puede asignar a empresas un usuario con rol CONTABLE");
        }
        if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId)) {
            throw new BusinessRuleException("El usuario ya esta asignado a esta empresa");
        }

        usuarioEmpresaRepository.save(UsuarioEmpresa.builder()
                .usuarioId(usuarioId)
                .empresaId(empresaId)
                .build());
    }

    @Transactional
    public void desasignar(UUID empresaId, UUID usuarioId) {
        UsuarioEmpresa asignacion = usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no esta asignado a esta empresa"));
        usuarioEmpresaRepository.delete(asignacion);
    }

    public List<ContableAsignadoResponse> listarAsignados(UUID empresaId) {
        List<UsuarioEmpresa> asignaciones = usuarioEmpresaRepository.findByEmpresaId(empresaId);

        Map<UUID, Usuario> usuariosPorId = usuarioRepository
                .findAllById(asignaciones.stream().map(UsuarioEmpresa::getUsuarioId).toList())
                .stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        return asignaciones.stream()
                .map(a -> {
                    Usuario usuario = usuariosPorId.get(a.getUsuarioId());
                    return new ContableAsignadoResponse(
                            a.getUsuarioId(), usuario.getEmail(), usuario.getNombre(), a.getFechaAsignacion());
                })
                .toList();
    }
}
