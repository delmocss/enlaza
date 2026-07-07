package com.enlaza.factura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * numero y hashActual se asignan atomicamente al crear, bajo lock
 * PESSIMISTIC_WRITE sobre la fila de SerieFacturacion (ver D5/D6 del modelo
 * de datos) -- es el punto exacto de riesgo de condicion de carrera del
 * proyecto. Solo EMITIDA/ANULADA, sin borradores (D7): anular no borra ni
 * renumera, conserva numero y hash para no romper la cadena.
 */
@Entity
@Table(name = "factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "serie_id", nullable = false)
    private UUID serieId;

    @Column(nullable = false)
    private int numero;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado;

    @Column(name = "hash_anterior", nullable = false)
    private String hashAnterior;

    @Column(name = "hash_actual", nullable = false)
    private String hashActual;

    @Column(name = "base_imponible", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "total_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIva;

    @Column(name = "total_irpf", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIrpf;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private UUID creadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
