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
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of } from 'rxjs';
import { AdminApi, Page, Params, Product, Row, Warehouse } from '../shared/admin-api';
import { RemoteState, errorMessage } from '../shared/remote-state';
import { DataTable } from '../shared/data-table';
import { RecordDetail } from '../shared/record-detail';
import { display, todayLima, daysBefore, periodParams } from '../shared/presentation';
import { METRICS, PurchaseTab, QUERY_COLUMNS, QUERY_TITLES, QueryKind } from './query.config';
@Component({
  selector: 'app-admin-queries',
  imports: [ReactiveFormsModule, RouterLink, DataTable, RecordDetail],
  templateUrl: './admin-queries.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminQueries {
  private readonly api = inject(AdminApi);
  private readonly destroy = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  readonly kind = signal<QueryKind>('movimientos');
  readonly tab = signal<PurchaseTab>('reposicion');
  readonly selected = signal<Row | null>(null);
  readonly results = new RemoteState<Page>(this.destroy);
  readonly metrics = new RemoteState<Row[]>(this.destroy);
  readonly stock = new RemoteState<Row>(this.destroy);
  readonly receptions = new RemoteState<Page>(this.destroy);
  readonly warehouses = new RemoteState<Warehouse[]>(this.destroy);
  readonly products = new RemoteState<Product[]>(this.destroy);
  readonly error = signal('');
  readonly page = signal(0);
  readonly receptionPage = signal(0);
  readonly context = signal('');
  readonly detailDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailDialog');
  readonly show = display;
  readonly receptionColumns = QUERY_COLUMNS['recepciones'];
  readonly form = inject(FormBuilder).nonNullable.group({
    productoId: '',
    almacenId: '',
    sku: '',
    desde: daysBefore(todayLima(), 29),
    hasta: todayLima(),
    estado: '',
    tipo: 'rotacion',
  });
  readonly title = computed(() => QUERY_TITLES[this.kind()]);
  readonly needsDates = computed(
    () =>
      this.kind() !== 'existencias' && !(this.kind() === 'compras' && this.tab() === 'reposicion'),
  );
  readonly loading = computed(
    () => this.results.loading() || this.metrics.loading() || this.stock.loading(),
  );
  readonly failure = computed(
    () => this.error() || this.results.error() || this.metrics.error() || this.stock.error(),
  );
  readonly states = computed(() =>
    this.tab() === 'solicitudes'
      ? ['PENDIENTE', 'APROBADA', 'RECHAZADA']
      : ['EMITIDA', 'ATENDIDA', 'CANCELADA'],
  );
  readonly currentColumns = signal(QUERY_COLUMNS['movimientos']);
  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.clear();
    });
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      this.kind.set(data['query'] as QueryKind);
      this.tab.set('reposicion');
      this.form.controls.estado.setValue('', { emitEvent: false });
      this.clear();
      this.loadOptions();
      if (this.kind() === 'movimientos' || this.kind() === 'compras') this.query();
    });
  }
  private clear(): void {
    this.results.clear();
    this.metrics.clear();
    this.stock.clear();
    this.error.set('');
    this.page.set(0);
    this.context.set('');
  }
  loadOptions(): void {
    this.warehouses.load(this.api.get<Warehouse[]>('almacenes'));
    if (this.kind() === 'existencias') this.products.load(this.api.products());
  }
  changeTab(tab: PurchaseTab): void {
    this.tab.set(tab);
    this.form.controls.estado.setValue('', { emitEvent: false });
    this.clear();
    this.query();
  }
  query(page = 0): void {
    if (this.loading()) return;
    const filters = this.form.getRawValue();
    this.error.set('');
    this.page.set(page);
    const params: Params = {
      sku: filters.sku.trim().toUpperCase(),
      almacenId: filters.almacenId,
      pagina: page,
      tamanio: 20,
    };
    if (this.needsDates()) {
      try {
        Object.assign(params, periodParams(filters.desde, filters.hasta));
        this.context.set(
          `Periodo: ${filters.desde} al ${filters.hasta}, días completos en horario de Lima.`,
        );
      } catch (error) {
        this.error.set((error as Error).message);
        return;
      }
    } else this.context.set('');
    if (this.kind() === 'existencias') {
      if (
        !Number.isSafeInteger(Number(filters.productoId)) ||
        Number(filters.productoId) <= 0 ||
        !Number.isSafeInteger(Number(filters.almacenId)) ||
        Number(filters.almacenId) <= 0
      ) {
        this.error.set('Selecciona un producto y un almacén para consultar su stock.');
        return;
      }
      this.stock.load(
        this.api.get<Row>('existencias', {
          productoId: filters.productoId,
          almacenId: filters.almacenId,
        }),
      );
      return;
    }
    if (this.kind() === 'indicadores') {
      this.metrics.load(
        forkJoin(
          METRICS.map((metric) =>
            this.api
              .get<Row>('indicadores', { ...params, codigo: metric.codigo })
              .pipe(
                catchError((error) =>
                  of({ codigo: metric.codigo, nombre: metric.nombre, error: errorMessage(error) }),
                ),
              ),
          ),
        ),
      );
      return;
    }
    let path = 'movimientos';
    let columns = QUERY_COLUMNS['movimientos'];
    if (this.kind() === 'compras') {
      path =
        this.tab() === 'reposicion'
          ? 'reposicion'
          : this.tab() === 'solicitudes'
            ? 'solicitudes-compra'
            : 'ordenes-compra';
      columns = QUERY_COLUMNS[this.tab()];
      if (this.tab() !== 'reposicion') params['estado'] = filters.estado;
    } else if (this.kind() === 'reportes') {
      path = `reportes/${filters.tipo}`;
      columns = QUERY_COLUMNS[filters.tipo];
    }
    this.currentColumns.set(columns);
    this.results.load(this.api.get<Page>(path, params));
  }
  inspect(row: Row): void {
    this.selected.set(row);
    this.receptions.clear();
    this.detailDialog().nativeElement.showModal();
    if (this.kind() === 'compras' && this.tab() === 'ordenes') this.loadReceptions(0);
  }
  loadReceptions(page: number): void {
    const id = this.selected()?.['id'];
    if (!id) return;
    this.receptionPage.set(page);
    this.receptions.load(
      this.api.get<Page>(`ordenes-compra/${Number(id)}/recepciones`, { pagina: page, tamanio: 20 }),
    );
  }
  closeDetail(): void {
    this.receptions.clear();
    this.detailDialog().nativeElement.close();
  }
  productName(id: unknown): string {
    const p = this.products.data()?.find((p) => p.id === id);
    return p ? `${p.sku} · ${p.marca} ${p.modelo}` : String(id);
  }
  warehouseName(id: unknown): string {
    const w = this.warehouses.data()?.find((w) => w.id === Number(id));
    return w ? `${w.codigo} · ${w.nombre}` : String(id);
  }
}
