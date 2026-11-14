import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminApi, Row } from '../shared/admin-api';
import { RemoteState } from '../shared/remote-state';
import { DataTable, Column } from '../shared/data-table';
import { RecordDetail } from '../shared/record-detail';
import { display } from '../shared/presentation';
export function object(value: unknown): Row {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Row) : {};
}
@Component({
  selector: 'app-admin-system',
  imports: [DataTable, RecordDetail],
  templateUrl: './admin-system.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSystem {
  private readonly api = inject(AdminApi);
  private readonly destroy = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  readonly kind = signal<'evidencias' | 'estado'>('estado');
  readonly type = signal('todos');
  readonly response = new RemoteState<Row>(this.destroy);
  readonly selected = signal<Row | null>(null);
  readonly detail = viewChild.required<ElementRef<HTMLDialogElement>>('detail');
  readonly show = display;
  readonly asObject = object;
  readonly groups = computed(() => {
    const data = this.response.data();
    if (!data || data['sinDatos']) return [];
    const keys =
      this.type() === 'todos'
        ? ['requisitos', 'reglas', 'seguridad', 'e2e', 'pruebasSeguridad', 'indicadoresTecnicos']
        : ['resultado'];
    return keys
      .filter((key) => data[key] !== undefined)
      .map((key) => ({
        key,
        title:
          {
            requisitos: 'Requisitos',
            reglas: 'Reglas de negocio',
            seguridad: 'Contratos de seguridad',
            e2e: 'Flujos de prueba',
            pruebasSeguridad: 'Pruebas de seguridad',
            indicadoresTecnicos: 'Indicadores técnicos',
            resultado: 'Resultados',
          }[key] ?? key,
        rows: (Array.isArray(data[key]) ? data[key] : [data[key]]).map(object),
      }));
  });
  constructor() {
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      this.kind.set(data['system']);
      this.type.set('todos');
      this.load();
    });
  }
  load(): void {
    this.response.load(
      this.api.get<Row>(
        this.kind() === 'estado' ? 'salud' : 'evidencias',
        this.kind() === 'evidencias' ? { tipo: this.type() } : {},
      ),
    );
  }
  selectType(event: Event): void {
    this.type.set((event.target as HTMLSelectElement).value);
    this.load();
  }
  columns(rows: Row[]): Column[] {
    const sample = rows[0] ?? {};
    return ['id', 'codigo', 'metodo', 'ruta', 'nombre', 'aprobado', 'estado', 'valor', 'unidad']
      .filter((key) => key in sample)
      .map((key) => ({
        key,
        label: (
          {
            id: 'ID',
            codigo: 'Código',
            metodo: 'Método',
            ruta: 'Ruta',
            nombre: 'Nombre',
            aprobado: 'Aprobado',
            estado: 'Estado',
            valor: 'Valor',
            unidad: 'Unidad',
          } as Record<string, string>
        )[key],
      }));
  }
  inspect(row: Row): void {
    this.selected.set(row);
    this.detail().nativeElement.showModal();
  }
}
