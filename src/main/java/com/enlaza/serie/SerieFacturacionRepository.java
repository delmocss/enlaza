package com.enlaza.serie;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SerieFacturacionRepository extends JpaRepository<SerieFacturacion, UUID> {

    List<SerieFacturacion> findByEmpresaId(UUID empresaId);

    Optional<SerieFacturacion> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);
}
