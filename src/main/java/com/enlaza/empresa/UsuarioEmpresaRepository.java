package com.enlaza.empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UUID> {

    List<UsuarioEmpresa> findByEmpresaId(UUID empresaId);

    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    boolean existsByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);
}
