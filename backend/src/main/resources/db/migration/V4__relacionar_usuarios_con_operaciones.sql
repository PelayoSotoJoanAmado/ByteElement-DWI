-- Conservar autor/confirmado_por como nombres históricos y añadir identidad referencial.
-- Los datos V3 fueron registrados con el username autenticado. Se vinculan sin
-- inventar cuentas ni reasignar operaciones a un administrador arbitrario.
ALTER TABLE movimiento ADD COLUMN usuario_id BIGINT;
ALTER TABLE conteo ADD COLUMN usuario_id BIGINT;
ALTER TABLE conteo ADD COLUMN confirmado_por_id BIGINT;
ALTER TABLE corte_stock ADD COLUMN usuario_id BIGINT;

UPDATE movimiento SET usuario_id = (
    SELECT u.id FROM usuario u WHERE u.username = movimiento.autor
);
UPDATE conteo SET usuario_id = (
    SELECT u.id FROM usuario u WHERE u.username = conteo.autor
);
UPDATE conteo SET confirmado_por_id = (
    SELECT u.id FROM usuario u WHERE u.username = conteo.confirmado_por
) WHERE confirmado = TRUE;
UPDATE corte_stock SET usuario_id = (
    SELECT u.id FROM usuario u WHERE u.username = corte_stock.autor
);

-- Si hubiera autores manuales sin cuenta, la migración falla en vez de borrar
-- historial o atribuirlo a otra persona. Ver APF2-MINIMO.md antes de aplicar V4.
ALTER TABLE movimiento ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE conteo ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE corte_stock ALTER COLUMN usuario_id SET NOT NULL;

ALTER TABLE movimiento ADD CONSTRAINT fk_movimiento_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario(id);
ALTER TABLE conteo ADD CONSTRAINT fk_conteo_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario(id);
ALTER TABLE conteo ADD CONSTRAINT fk_conteo_confirmado_por
    FOREIGN KEY (confirmado_por_id) REFERENCES usuario(id);
ALTER TABLE corte_stock ADD CONSTRAINT fk_corte_stock_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario(id);
ALTER TABLE conteo ADD CONSTRAINT ck_conteo_usuario_confirmacion
    CHECK ((confirmado = FALSE AND confirmado_por_id IS NULL)
        OR (confirmado = TRUE AND confirmado_por_id IS NOT NULL));

CREATE INDEX ix_movimiento_usuario ON movimiento(usuario_id);
CREATE INDEX ix_conteo_usuario ON conteo(usuario_id);
CREATE INDEX ix_conteo_confirmado_por ON conteo(confirmado_por_id);
CREATE INDEX ix_corte_stock_usuario ON corte_stock(usuario_id);
