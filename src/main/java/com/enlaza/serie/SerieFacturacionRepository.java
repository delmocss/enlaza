package com.enlaza.serie;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SerieFacturacionRepository extends JpaRepository<SerieFacturacion, UUID> {

    List<SerieFacturacion> findByEmpresaId(UUID empresaId);

    Optional<SerieFacturacion> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    /**
     * Lock exclusivo sobre la fila de la serie: dentro de la misma
     * transaccion que crea una Factura, esto bloquea a cualquier otra
     * transaccion que intente numerar en la misma serie hasta que esta
     * termine (commit o rollback) -- evita numeros duplicados o huecos por
     * dos facturas concurrentes leyendo el mismo ultimoNumero (D5 del
     * modelo de datos).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerieFacturacion s WHERE s.id = :id AND s.empresaId = :empresaId")
    Optional<SerieFacturacion> findByIdAndEmpresaIdForUpdate(@Param("id") UUID id, @Param("empresaId") UUID empresaId);
}
