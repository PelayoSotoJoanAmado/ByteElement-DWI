import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Row } from './admin-api';
import { display } from './presentation';
const TITLES: Record<string, string> = {
  id: 'ID',
  origenAlmacenId: 'Almacén de origen',
  destinoAlmacenId: 'Almacén de destino',
  movimientoSalidaId: 'Movimiento de salida',
  movimientoEntradaId: 'Movimiento de entrada',
  saldoOrigenResultante: 'Saldo del origen',
  saldoDestinoResultante: 'Saldo del destino',
  cantidadFisica: 'Cantidad física',
  saldoCorte: 'Saldo al corte',
  confirmado: 'Confirmado',
  confirmadoPor: 'Confirmado por',
  confirmadoEn: 'Fecha de confirmación',
  username: 'Usuario',
  rol: 'Rol',
  activo: 'Activo',
  sku: 'SKU',
  productoId: 'Producto',
  almacenId: 'Almacén',
  nombre: 'Nombre',
  codigo: 'Código',
  categoria: 'Categoría',
  marca: 'Marca',
  modelo: 'Modelo',
  especificacion: 'Especificación',
  critico: 'Crítico',
  tipo: 'Tipo',
  cantidad: 'Cantidad',
  autor: 'Autor',
  fecha: 'Fecha',
  motivo: 'Motivo',
  saldoResultante: 'Saldo resultante',
  montoEstimado: 'Monto estimado (S/)',
  estado: 'Estado',
  solicitanteId: 'Solicitante',
  enviadaEn: 'Fecha de envío',
  aprobadaEn: 'Fecha de aprobación',
  revisadaEn: 'Fecha de revisión',
  revisadoPorId: 'Revisado por',
  motivoRechazo: 'Motivo de rechazo',
  justificacionAprobacion: 'Justificación de aprobación',
  urgente: 'Urgente',
  motivoUrgencia: 'Motivo de urgencia',
  duplicadaDeId: 'Solicitud original',
  duplicidadConfirmadaEn: 'Duplicidad confirmada',
  duplicidadConfirmadaPorId: 'Confirmada por',
  motivoDuplicidad: 'Motivo de duplicidad',
  similaresAbiertas: 'Solicitudes similares abiertas',
  solicitudId: 'Solicitud',
  numeroOrden: 'Número de orden',
  emitidaEn: 'Fecha de emisión',
  emitidaPorId: 'Emitida por',
  cantidadRecibida: 'Cantidad recibida',
  cantidadPendiente: 'Cantidad pendiente',
  canceladaEn: 'Fecha de cancelación',
  motivoCancelacion: 'Motivo de cancelación',
  ordenId: 'Orden',
  movimientoId: 'Movimiento',
  usuarioId: 'Usuario',
  stock: 'Stock actual',
  minimo: 'Mínimo',
  maximo: 'Máximo',
  consumo: 'Consumo',
  consumoDiario: 'Consumo diario',
  pedidosPendientes: 'Pedidos pendientes',
  posicionInventario: 'Posición de inventario',
  puntoReposicion: 'Punto de reposición',
  cantidadSugerida: 'Cantidad sugerida',
  urgenciaSugerida: 'Urgencia sugerida',
  desde: 'Desde',
  hasta: 'Hasta (exclusivo)',
  ventanaDias: 'Ventana (días)',
  plazoDias: 'Plazo (días)',
  seguridadDias: 'Seguridad (días)',
  regla: 'Regla aplicada',
  fechaCorte: 'Fecha del corte',
  lote: 'Lote del corte',
  rotacion: 'Rotación',
  stockPromedio: 'Stock promedio',
  numerador: 'Numerador',
  denominador: 'Denominador',
  valor: 'Valor',
  unidad: 'Unidad',
  sinDatos: 'Sin datos',
  aprobado: 'Aprobado',
  pruebas: 'Pruebas',
  ruta: 'Ruta',
  metodo: 'Método',
  rolesPermitidos: 'Roles permitidos',
  rolesDenegados: 'Roles denegados',
  tokenAusente: 'Sin token',
  tokenInvalido: 'Token inválido',
};
@Component({
  selector: 'app-record-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<dl class="ap-details">
    @for (entry of entries(); track entry[0]) {
      <div>
        <dt>{{ title(entry[0]) }}</dt>
        <dd>
          <pre>{{ value(entry[0], entry[1]) }}</pre>
        </dd>
      </div>
    }
  </dl>`,
})
export class RecordDetail {
  readonly record = input.required<Row>();
  entries(): [string, unknown][] {
    return Object.entries(this.record());
  }
  title(key: string): string {
    return TITLES[key] ?? key;
  }
  value(key: string, value: unknown): string {
    return display(
      value,
      /fecha|En$/.test(key) && typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)
        ? 'date'
        : undefined,
    );
  }
}
