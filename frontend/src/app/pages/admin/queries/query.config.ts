import { Column } from '../shared/data-table';
export type QueryKind = 'existencias' | 'movimientos' | 'compras' | 'reportes' | 'indicadores';
export type PurchaseTab = 'reposicion' | 'solicitudes' | 'ordenes';
export const QUERY_TITLES: Record<QueryKind, string> = {
  existencias: 'Existencias',
  movimientos: 'Movimientos',
  compras: 'Compras y reposición',
  reportes: 'Reportes de inventario',
  indicadores: 'Indicadores de inventario',
};
export const METRICS = [
  { codigo: 'I05', nombre: 'Quiebre en el período' },
  { codigo: 'I06', nombre: 'Compras urgentes' },
  { codigo: 'I07', nombre: 'Coincidencia de inventario' },
  { codigo: 'I08', nombre: 'Tiempo promedio de aprobación' },
  { codigo: 'I09', nombre: 'Solicitudes duplicadas confirmadas' },
  { codigo: 'I10', nombre: 'Productos sobre el máximo al corte' },
];
export const QUERY_COLUMNS: Record<string, Column[]> = {
  movimientos: [
    { key: 'id', label: 'ID' },
    { key: 'fecha', label: 'Fecha (Lima)', format: 'date' },
    { key: 'productoId', label: 'Producto' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'tipo', label: 'Tipo' },
    { key: 'cantidad', label: 'Cantidad' },
    { key: 'saldoResultante', label: 'Saldo' },
    { key: 'autor', label: 'Autor' },
  ],
  reposicion: [
    { key: 'sku', label: 'SKU' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'stock', label: 'Stock' },
    { key: 'minimo', label: 'Mínimo' },
    { key: 'maximo', label: 'Máximo' },
    { key: 'pedidosPendientes', label: 'Pedidos pendientes' },
    { key: 'cantidadSugerida', label: 'Sugerida' },
    { key: 'estado', label: 'Estado', format: 'status' },
  ],
  solicitudes: [
    { key: 'id', label: 'Solicitud' },
    { key: 'sku', label: 'SKU' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'cantidad', label: 'Cantidad' },
    { key: 'montoEstimado', label: 'Estimado (S/)' },
    { key: 'estado', label: 'Estado', format: 'status' },
    { key: 'enviadaEn', label: 'Enviada (Lima)', format: 'date' },
  ],
  ordenes: [
    { key: 'numeroOrden', label: 'Orden' },
    { key: 'solicitudId', label: 'Solicitud' },
    { key: 'estado', label: 'Estado', format: 'status' },
    { key: 'cantidad', label: 'Cantidad' },
    { key: 'cantidadRecibida', label: 'Recibida' },
    { key: 'cantidadPendiente', label: 'Pendiente' },
    { key: 'emitidaEn', label: 'Emitida (Lima)', format: 'date' },
  ],
  rotacion: [
    { key: 'sku', label: 'SKU' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'consumo', label: 'Consumo' },
    { key: 'stockPromedio', label: 'Stock promedio' },
    { key: 'rotacion', label: 'Rotación' },
  ],
  'stock-critico': [
    { key: 'sku', label: 'SKU' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'cantidad', label: 'Cantidad' },
    { key: 'minimo', label: 'Mínimo' },
    { key: 'maximo', label: 'Máximo' },
    { key: 'estado', label: 'Estado', format: 'status' },
    { key: 'fechaCorte', label: 'Fecha del corte', format: 'date' },
  ],
  recepciones: [
    { key: 'id', label: 'Recepción' },
    { key: 'fecha', label: 'Fecha (Lima)', format: 'date' },
    { key: 'cantidad', label: 'Cantidad' },
    { key: 'movimientoId', label: 'Movimiento' },
    { key: 'motivo', label: 'Motivo' },
  ],
};
