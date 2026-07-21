package com.enlaza.empresa;

import com.enlaza.empresa.dto.ContableAsignadoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UUID> {

    List<UsuarioEmpresa> findByEmpresaId(UUID empresaId);

    List<UsuarioEmpresa> findByUsuarioId(UUID usuarioId);

    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    boolean existsByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    /**
     * INNER JOIN por igualdad de UUID (sin relacion JPA declarada, ver
     * decision de estilo del proyecto en Usuario.clienteId): de paso excluye
     * cualquier fila de usuario_empresa cuyo usuario_id ya no tenga Usuario,
     * en vez de arriesgar un NPE al mapear la respuesta.
     */
    @Query("""
            SELECT new com.enlaza.empresa.dto.ContableAsignadoResponse(u.id, u.email, u.nombre, ue.fechaAsignacion)
            FROM UsuarioEmpresa ue, Usuario u
            WHERE ue.empresaId = :empresaId AND ue.usuarioId = u.id
            """)
    List<ContableAsignadoResponse> findAsignadosConDatosUsuario(@Param("empresaId") UUID empresaId);
}
