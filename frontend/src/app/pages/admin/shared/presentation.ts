export const ROLE_LABELS: Record<string, string> = {
  ALMACEN: 'Almacén',
  COMPRAS: 'Compras',
  SUPERVISOR: 'Supervisor',
  CONSULTA_COMERCIAL: 'Consulta comercial',
  ADMINISTRADOR: 'Administrador',
};
const LABELS: Record<string, string> = {
  ...ROLE_LABELS,
  true: 'Sí',
  false: 'No',
  ENTRADA: 'Entrada',
  SALIDA: 'Salida',
  AJUSTE_ENTRADA: 'Ajuste de entrada',
  AJUSTE_SALIDA: 'Ajuste de salida',
  TRANSFERENCIA_ENTRADA: 'Transferencia recibida',
  TRANSFERENCIA_SALIDA: 'Transferencia enviada',
  PENDIENTE: 'Pendiente',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  EMITIDA: 'Emitida',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
  QUIEBRE: 'Quiebre',
  BAJO_MINIMO: 'Bajo mínimo',
  SOBRE_MAXIMO: 'Sobre máximo',
  SIN_POLITICA: 'Sin política',
  NORMAL: 'Normal',
  UP: 'Disponible',
};
export function display(value: unknown, format?: string): string {
  if (value === null || value === undefined || value === '') return '—';
  if (format === 'active') return value ? 'Activo' : 'Inactivo';
  if (format === 'date') {
    const date = new Date(String(value));
    return Number.isNaN(date.getTime())
      ? '—'
      : new Intl.DateTimeFormat('es-PE', {
          dateStyle: 'short',
          timeStyle: 'short',
          timeZone: 'America/Lima',
        }).format(date);
  }
  if (typeof value === 'number')
    return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 4 }).format(value);
  if (typeof value === 'object') return JSON.stringify(value, null, 2);
  return LABELS[String(value)] ?? String(value);
}
export function todayLima(): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
}
export function daysBefore(date: string, days: number): string {
  return new Date(Date.parse(`${date}T12:00:00Z`) - days * 86400000).toISOString().slice(0, 10);
}
export function periodParams(from: string, through: string): { desde: string; hasta: string } {
  const valid = (value: string) =>
    /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    !Number.isNaN(Date.parse(`${value}T00:00:00Z`)) &&
    new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;
  if (!valid(from) || !valid(through) || from > through)
    throw new Error(
      'Selecciona un periodo válido: la fecha final debe ser igual o posterior a la inicial.',
    );
  return {
    desde: new Date(`${from}T00:00:00-05:00`).toISOString(),
    hasta: new Date(`${daysBefore(through, -1)}T00:00:00-05:00`).toISOString(),
  };
}
