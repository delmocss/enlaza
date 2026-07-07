package com.enlaza.serie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Serie de numeracion de una Empresa (ej: "A", "F2026"). ultimoNumero y
 * ultimoHash se leen y actualizan bajo PESSIMISTIC_WRITE al emitir una
 * factura (Fase 5) -- es el punto exacto de la cadena de hashes, ver
 * decisiones D5/D6 del modelo de datos (docs/01-modelo-datos.md).
 */
@Entity
@Table(name = "serie_facturacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieFacturacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private String codigo;

    @Column(name = "ultimo_numero", nullable = false)
    private int ultimoNumero;

    @Column(name = "ultimo_hash", nullable = false)
    private String ultimoHash;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
