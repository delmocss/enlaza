CREATE TABLE factura (
    id               UUID PRIMARY KEY,
    serie_id         UUID          NOT NULL REFERENCES serie_facturacion (id),
    numero           INTEGER       NOT NULL,
    fecha_emision    TIMESTAMP     NOT NULL,
    cliente_id       UUID          NOT NULL REFERENCES cliente (id),
    estado           VARCHAR(20)   NOT NULL,
    hash_anterior    VARCHAR(64)   NOT NULL,
    hash_actual      VARCHAR(64)   NOT NULL,
    base_imponible   NUMERIC(19,2) NOT NULL,
    total_iva        NUMERIC(19,2) NOT NULL,
    total_irpf       NUMERIC(19,2) NOT NULL,
    total            NUMERIC(19,2) NOT NULL,
    creado_por       UUID          NOT NULL REFERENCES usuario (id),
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    UNIQUE (serie_id, numero)
);

CREATE INDEX idx_factura_cliente_id ON factura (cliente_id);

CREATE TABLE linea_factura (
    id                 UUID PRIMARY KEY,
    factura_id         UUID          NOT NULL REFERENCES factura (id),
    descripcion        VARCHAR(500)  NOT NULL,
    cantidad           NUMERIC(19,2) NOT NULL,
    precio_unitario    NUMERIC(19,2) NOT NULL,
    porcentaje_iva     NUMERIC(5,2)  NOT NULL,
    porcentaje_irpf    NUMERIC(5,2)  NOT NULL,
    importe_base       NUMERIC(19,2) NOT NULL,
    importe_iva        NUMERIC(19,2) NOT NULL,
    importe_irpf       NUMERIC(19,2) NOT NULL,
    importe_total      NUMERIC(19,2) NOT NULL
);

CREATE INDEX idx_linea_factura_factura_id ON linea_factura (factura_id);
