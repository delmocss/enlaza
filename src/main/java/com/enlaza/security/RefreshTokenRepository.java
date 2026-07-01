package com.enlaza.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUsuarioIdAndRevokedFalse(UUID usuarioId);

    /**
     * UPDATE condicional atomico: revoca el token solo si seguia sin revocar.
     * Devuelve 1 si esta llamada "gano" la carrera, 0 si otra ya lo revoco
     * primero -- eso es lo que impide que dos rotaciones concurrentes del
     * mismo token emitan cada una un token hijo valido.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.id = :id AND r.revoked = false")
    int revokeIfNotRevoked(@Param("id") UUID id);
}
