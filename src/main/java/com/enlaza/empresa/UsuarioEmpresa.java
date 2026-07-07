package com.enlaza.empresa;

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
 * Asignacion N-M entre un Usuario con rol CONTABLE y una Empresa que puede
 * operar. Un ADMIN no necesita fila aqui: ve todas las empresas por rol.
 */
@Entity
@Table(name = "usuario_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private Instant fechaAsignacion;

    @PrePersist
    void onCreate() {
        fechaAsignacion = Instant.now();
    }
}
