-- Ejecutar con psql -v ON_ERROR_STOP=1 -f cargar-volumen.sql en una BD de pruebas.
-- Datos sinteticos, no una medicion real de ByteElement. No elimina datos existentes.
BEGIN;
SELECT pg_advisory_xact_lock(704310000);
DO $$
BEGIN
 IF EXISTS(SELECT 1 FROM producto WHERE sku LIKE 'VOL-%') THEN
   RAISE EXCEPTION 'Ya existen SKU VOL-. Use otra base de pruebas; no se acumula la carga.';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE rol='ALMACEN' AND activo)
 OR NOT EXISTS(SELECT 1 FROM usuario WHERE rol='COMPRAS' AND activo)
 OR NOT EXISTS(SELECT 1 FROM usuario WHERE rol='SUPERVISOR' AND activo) THEN
   RAISE EXCEPTION 'Se requieren usuarios activos ALMACEN, COMPRAS y SUPERVISOR.';
 END IF;
 IF (SELECT count(*) FROM almacen WHERE codigo IN ('A01','A02','A03') AND activo)<>3 THEN
   RAISE EXCEPTION 'Se requieren A01, A02 y A03 activos.';
 END IF;
END $$;
CREATE TEMP TABLE vol_actores ON COMMIT DROP AS
SELECT (SELECT id FROM usuario WHERE rol='ALMACEN' AND activo ORDER BY id LIMIT 1) almacen,
 (SELECT id FROM usuario WHERE rol='COMPRAS' AND activo ORDER BY id LIMIT 1) compras,
 (SELECT id FROM usuario WHERE rol='SUPERVISOR' AND activo ORDER BY id LIMIT 1) supervisor;
INSERT INTO producto(sku,categoria,marca,modelo,especificacion,critico,activo)
SELECT 'VOL-'||lpad(i::text,5,'0'),'RAM','Sintetica','Modelo '||i,'Prueba de volumen',true,true
FROM generate_series(1,10000) i;
CREATE TEMP TABLE vol_productos ON COMMIT DROP AS
SELECT id,sku,right(sku,5)::integer n FROM producto WHERE sku LIKE 'VOL-%';
CREATE TEMP TABLE vol_pares ON COMMIT DROP AS
SELECT p.id producto_id,p.sku,p.n,a.id almacen_id,
 CASE WHEN p.n%10=0 THEN 0 WHEN p.n%10=1 THEN 150 ELSE 30 END::bigint saldo
FROM vol_productos p CROSS JOIN almacen a WHERE a.codigo IN ('A01','A02','A03');
INSERT INTO politica_stock(producto_id,almacen_id,minimo,maximo)
SELECT producto_id,almacen_id,10,100 FROM vol_pares;
INSERT INTO existencia(producto_id,almacen_id,cantidad,version)
SELECT producto_id,almacen_id,saldo,0 FROM vol_pares;
INSERT INTO movimiento(producto_id,almacen_id,tipo,cantidad,autor,fecha,motivo,saldo_resultante,referencia,usuario_id)
SELECT p.producto_id,p.almacen_id,'ENTRADA',200,u.username,now()-interval '30 days','Saldo inicial sintetico',200,
 'VOL-IN-'||p.producto_id||'-'||p.almacen_id,u.id
FROM vol_pares p CROSS JOIN vol_actores v JOIN usuario u ON u.id=v.almacen;
INSERT INTO movimiento(producto_id,almacen_id,tipo,cantidad,autor,fecha,motivo,saldo_resultante,referencia,usuario_id)
SELECT p.producto_id,p.almacen_id,'SALIDA',200-p.saldo,u.username,now()-interval '1 day','Consumo sintetico',p.saldo,
 'VOL-OUT-'||p.producto_id||'-'||p.almacen_id,u.id
FROM vol_pares p CROSS JOIN vol_actores v JOIN usuario u ON u.id=v.almacen;
INSERT INTO conteo(producto_id,almacen_id,movimiento_corte_id,cantidad_fisica,saldo_corte,fecha_corte,autor,motivo,usuario_id)
SELECT p.producto_id,p.almacen_id,m.id,p.saldo+CASE WHEN p.n%10=0 THEN 1 ELSE 0 END,p.saldo,now(),u.username,
 'Control sintetico',u.id
FROM vol_pares p JOIN almacen a ON a.id=p.almacen_id
JOIN movimiento m ON m.referencia='VOL-OUT-'||p.producto_id||'-'||p.almacen_id
CROSS JOIN vol_actores v JOIN usuario u ON u.id=v.almacen
WHERE p.n<=1000 AND a.codigo='A01';
INSERT INTO solicitud_compra(usuario_id,producto_id,almacen_id,cantidad,motivo,estado,enviada_en,aprobada_en,
 revisado_por_id,clave_idempotencia,monto_estimado,urgente,motivo_urgencia,revisada_en)
SELECT v.compras,p.id,a.id,10,'Reposicion sintetica','APROBADA',now()-interval '5 days',now()-interval '3 days',
 v.supervisor,'VOL-S-'||p.n,100,p.n%5=0,CASE WHEN p.n%5=0 THEN 'Urgencia simulada' END,now()-interval '3 days'
FROM vol_productos p CROSS JOIN vol_actores v CROSS JOIN almacen a WHERE p.n<=1840 AND a.codigo='A01';
INSERT INTO solicitud_compra(usuario_id,producto_id,almacen_id,cantidad,motivo,estado,enviada_en,
 revisado_por_id,clave_idempotencia,monto_estimado,duplicada_de_id,motivo_rechazo,revisada_en,
 duplicidad_confirmada_en,duplicidad_confirmada_por_id,motivo_duplicidad)
SELECT v.compras,p.id,a.id,10,'Necesidad repetida sintetica','RECHAZADA',now()-interval '4 days',
 v.supervisor,'VOL-D-'||p.n,100,s.id,'Duplicidad confirmada',now()-interval '3 days',
 now()-interval '3 days',v.supervisor,'Misma necesidad sintetica'
FROM vol_productos p CROSS JOIN vol_actores v CROSS JOIN almacen a
JOIN solicitud_compra s ON s.clave_idempotencia='VOL-S-'||p.n
WHERE p.n<=160 AND a.codigo='A01';
INSERT INTO orden_compra(solicitud_id,usuario_id,numero_orden,urgente,motivo_urgencia,emitida_en,estado,monto_estimado)
SELECT s.id,v.compras,'VOL-OC-'||p.n,s.urgente,s.motivo_urgencia,now()-interval '2 days','EMITIDA',s.monto_estimado
FROM vol_productos p JOIN solicitud_compra s ON s.clave_idempotencia='VOL-S-'||p.n CROSS JOIN vol_actores v;
INSERT INTO corte_stock(lote,fecha,autor,producto_id,almacen_id,sku,critico,cantidad,minimo,maximo,usuario_id)
SELECT '00000000-0000-4000-8000-000000010000',now(),u.username,p.producto_id,p.almacen_id,p.sku,true,p.saldo,10,100,u.id
FROM vol_pares p CROSS JOIN vol_actores v JOIN usuario u ON u.id=v.supervisor;
COMMIT;
ANALYZE;
SELECT 'productos' tabla,count(*) cantidad FROM producto WHERE sku LIKE 'VOL-%'
UNION ALL SELECT 'movimientos',count(*) FROM movimiento WHERE referencia LIKE 'VOL-%'
UNION ALL SELECT 'solicitudes',count(*) FROM solicitud_compra WHERE clave_idempotencia LIKE 'VOL-%';
