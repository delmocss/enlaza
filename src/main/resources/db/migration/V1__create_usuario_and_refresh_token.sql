CREATE TABLE usuario (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre        VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    -- Sin FK a cliente todavia: la tabla cliente se crea en la Fase 4.
    cliente_id    UUID,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_refresh_token_usuario_id ON refresh_token (usuario_id);
