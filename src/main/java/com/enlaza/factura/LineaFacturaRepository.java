package com.enlaza.factura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LineaFacturaRepository extends JpaRepository<LineaFactura, UUID> {

    List<LineaFactura> findByFacturaIdOrderByOrdenAsc(UUID facturaId);

    List<LineaFactura> findByFacturaIdInOrderByFacturaIdAscOrdenAsc(List<UUID> facturaIds);
}
