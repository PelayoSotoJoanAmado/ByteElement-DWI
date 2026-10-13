# Modelo relacional APF2

```mermaid
erDiagram
 USUARIO ||--o{ MOVIMIENTO : registra
 USUARIO ||--o{ CONTEO : registra
 USUARIO o|--o{ CONTEO : confirma
 USUARIO ||--o{ CORTE_STOCK : genera
 USUARIO ||--o{ SOLICITUD_COMPRA : solicita
 USUARIO o|--o{ SOLICITUD_COMPRA : revisa
 USUARIO ||--o{ ORDEN_COMPRA : emite
 USUARIO {
  bigint id PK
  string username UK
  string password_hash
  string rol
  boolean activo
 }
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
 PRODUCTO ||--o{ SOLICITUD_COMPRA : requiere
 ALMACEN ||--o{ SOLICITUD_COMPRA : destino
 SOLICITUD_COMPRA o|--o| SOLICITUD_COMPRA : duplicada_de
 SOLICITUD_COMPRA ||--o| ORDEN_COMPRA : genera
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
  bigint usuario_id FK
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
  bigint usuario_id FK
  bigint confirmado_por_id FK
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
  bigint usuario_id FK
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
 SOLICITUD_COMPRA {
  bigint id PK
  bigint usuario_id FK
  bigint producto_id FK
  bigint almacen_id FK
  bigint cantidad
  string motivo
  string estado
  timestamp enviada_en
  timestamp aprobada_en
  bigint revisado_por_id FK
  string motivo_rechazo
  bigint duplicada_de_id FK
  string clave_idempotencia
 }
 ORDEN_COMPRA {
  bigint id PK
  bigint solicitud_id FK
  bigint usuario_id FK
  string numero_orden UK
  boolean urgente
  string motivo_urgencia
  timestamp emitida_en
  string estado
  numeric monto_estimado
 }
```

`usuario` almacena id, username único, password_hash BCrypt, rol y activo. Las cuatro claves foráneas vinculan las operaciones al usuario autenticado y al supervisor que confirma. La auditoría conserva además los nombres como texto histórico para que un cambio posterior de cuenta no cambie el nombre registrado. El rol del JWT se contrasta con el usuario actual en cada solicitud.

Claves compuestas únicas: politica_stock(producto_id,almacen_id), existencia(producto_id,almacen_id) y corte_stock(lote,producto_id,almacen_id). No hay endpoints de borrado físico de movimientos, conteos ni cortes.

Las transferencias enlazan sus dos movimientos mediante la misma referencia UUID. Los ajustes enlazan el conteo al movimiento generado; movimiento_corte_id es un marcador del último movimiento al registrar el conteo y permite detectar que hubo operaciones posteriores incluso si el saldo final coincide.

Las migraciones SQL V1–V4 constituyen el modelo físico. Los movimientos operativos se representan como entidades `MovimientoPersistido`; el record `Movimiento` conserva el contrato de salida del prototipo APF1.

La migración V4 añade las relaciones sin modificar V1–V3 y vincula los registros previos por coincidencia exacta de `autor` / `confirmado_por` con `usuario.username`. Los autores son obligatorios; `confirmado_por_id` es nulo hasta confirmar y obligatorio al confirmar. Las FK no usan ON DELETE CASCADE: la base impide eliminar usuarios referenciados por el historial.
