# Modelo relacional APF2

```mermaid
erDiagram
 PRODUCTO ||--o{ POLITICA_STOCK : configura
 ALMACEN ||--o{ POLITICA_STOCK : aplica
 PRODUCTO ||--o{ EXISTENCIA : posee
 ALMACEN ||--o{ EXISTENCIA : conserva
 PRODUCTO ||--o{ MOVIMIENTO : registra
 ALMACEN ||--o{ MOVIMIENTO : recibe
 PRODUCTO ||--o{ CONTEO : verifica
 ALMACEN ||--o{ CONTEO : verifica
 MOVIMIENTO o|--o| CONTEO : ajuste
 PRODUCTO ||--o{ CORTE_STOCK : fotografia
 ALMACEN ||--o{ CORTE_STOCK : fotografia
 PRODUCTO {
  bigint id PK
  string sku UK
  boolean critico
  boolean activo
 }
 ALMACEN {
  bigint id PK
  string codigo UK
  boolean activo
 }
 POLITICA_STOCK {
  bigint id PK
  bigint producto_id FK
  bigint almacen_id FK
  bigint minimo
  bigint maximo
 }
 EXISTENCIA {
  bigint id PK
  bigint producto_id FK
  bigint almacen_id FK
  bigint cantidad
  bigint version
 }
 MOVIMIENTO {
  bigint id PK
  bigint producto_id FK
  bigint almacen_id FK
  string tipo
  bigint cantidad
  string autor
  timestamp fecha
  string motivo
  bigint saldo_resultante
  string referencia
 }
 CONTEO {
  bigint id PK
  bigint producto_id FK
  bigint almacen_id FK
  bigint movimiento_corte_id
  bigint cantidad_fisica
  bigint saldo_corte
  timestamp fecha_corte
  string autor
  boolean confirmado
  string confirmado_por
  timestamp confirmado_en
  bigint movimiento_id FK
 }
 CORTE_STOCK {
  bigint id PK
  string lote
  timestamp fecha
  string autor
  bigint producto_id FK
  bigint almacen_id FK
  string sku
  boolean critico
  bigint cantidad
  bigint minimo
  bigint maximo
 }
```

`usuario` almacena id, username único, password_hash BCrypt, rol y activo. La auditoría operativa conserva el nombre autenticado como texto histórico para que un cambio futuro de cuenta no altere el movimiento. El rol del JWT se contrasta con el usuario actual en cada solicitud.

Claves compuestas únicas: politica_stock(producto_id,almacen_id), existencia(producto_id,almacen_id) y corte_stock(lote,producto_id,almacen_id). No hay endpoints de borrado físico de movimientos, conteos ni cortes.

Las transferencias enlazan sus dos movimientos mediante la misma referencia UUID. Los ajustes enlazan el conteo al movimiento generado; movimiento_corte_id es un marcador del último movimiento al registrar el conteo y permite detectar que hubo operaciones posteriores incluso si el saldo final coincide.

Las migraciones SQL V1–V3 constituyen el modelo físico. Los movimientos operativos se representan como entidades `MovimientoPersistido`; el record `Movimiento` conserva el contrato de salida del prototipo APF1.
