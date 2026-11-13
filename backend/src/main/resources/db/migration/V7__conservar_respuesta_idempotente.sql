-- La creacion conserva su respuesta original, aunque luego cambie el estado del recurso.
ALTER TABLE solicitud_compra ADD COLUMN respuesta_creacion TEXT;
CREATE INDEX ix_conteo_fecha_par ON conteo(fecha_corte,producto_id,almacen_id);
