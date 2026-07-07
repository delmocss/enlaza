CREATE TABLE empresa (
    id            UUID PRIMARY KEY,
    razon_social  VARCHAR(255) NOT NULL,
    nif           VARCHAR(20)  NOT NULL UNIQUE,
    direccion     VARCHAR(255),
    email         VARCHAR(255),
    telefono      VARCHAR(30),
    activa        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

CREATE TABLE cliente (
    id           UUID PRIMARY KEY,
    empresa_id   UUID         NOT NULL REFERENCES empresa (id),
    nombre       VARCHAR(255) NOT NULL,
    nif          VARCHAR(20)  NOT NULL,
    direccion    VARCHAR(255),
    email        VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_cliente_empresa_id ON cliente (empresa_id);

-- Pendiente desde la Fase 3: la tabla cliente ya existe, se completa la FK fisica.
ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id);

CREATE TABLE usuario_empresa (
    id                UUID PRIMARY KEY,
    usuario_id        UUID NOT NULL REFERENCES usuario (id),
    empresa_id        UUID NOT NULL REFERENCES empresa (id),
    fecha_asignacion  TIMESTAMP NOT NULL,
    UNIQUE (usuario_id, empresa_id)
);

CREATE INDEX idx_usuario_empresa_empresa_id ON usuario_empresa (empresa_id);

CREATE TABLE serie_facturacion (
    id             UUID PRIMARY KEY,
    empresa_id     UUID         NOT NULL REFERENCES empresa (id),
    codigo         VARCHAR(20)  NOT NULL,
    ultimo_numero  INTEGER      NOT NULL DEFAULT 0,
    ultimo_hash    VARCHAR(64)  NOT NULL,
    activa         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    UNIQUE (empresa_id, codigo)
);
