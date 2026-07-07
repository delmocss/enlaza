package com.enlaza.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByEmpresaId(UUID empresaId);

    Optional<Cliente> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
