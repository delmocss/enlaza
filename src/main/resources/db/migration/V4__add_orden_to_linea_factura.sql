ALTER TABLE linea_factura ADD COLUMN orden INTEGER NOT NULL DEFAULT 0;

-- Backfill para filas ya existentes (creadas antes de que esta columna
-- existiera): ctid (posicion fisica de la fila) es la unica aproximacion
-- disponible al orden de insercion original, ya que linea_factura no
-- guarda ningun timestamp propio. Sin este backfill, toda factura
-- multi-linea creada antes de esta migracion quedaria con orden=0 en
-- todas sus lineas -- un ORDER BY orden ASC sin desempate no garantiza
-- devolver esas filas en su orden real, lo que rompe la verificacion de
-- cadena (Fase 6) para datos historicos que nunca fueron manipulados.
WITH filas_ordenadas AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY factura_id ORDER BY ctid) - 1 AS orden_reconstruido
    FROM linea_factura
)
UPDATE linea_factura
SET orden = filas_ordenadas.orden_reconstruido
FROM filas_ordenadas
WHERE linea_factura.id = filas_ordenadas.id;
