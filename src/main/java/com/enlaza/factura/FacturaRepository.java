package com.enlaza.factura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacturaRepository extends JpaRepository<Factura, UUID> {

    List<Factura> findBySerieId(UUID serieId);

    Optional<Factura> findByIdAndSerieId(UUID id, UUID serieId);

    List<Factura> findByClienteIdOrderByFechaEmisionDesc(UUID clienteId);
}
