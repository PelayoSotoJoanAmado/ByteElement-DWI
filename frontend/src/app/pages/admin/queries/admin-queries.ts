import { landingPath } from '../../../core/auth/auth.guards';
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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, timeout, forkJoin, of } from 'rxjs';
import { AdminApi, Page, Params, Product, Row, Warehouse } from '../shared/admin-api';
import { RemoteState, errorMessage } from '../shared/remote-state';
import { DataTable } from '../shared/data-table';
import { RecordDetail } from '../shared/record-detail';
import { display, todayLima, daysBefore, periodParams } from '../shared/presentation';
import { AuthService } from '../../../core/auth/auth.service';
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
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  readonly role = this.auth.user()!.rol;
  readonly home = landingPath(this.role);
  readonly warehouse = inject(AuthService).user()?.rol === 'ALMACEN';
  readonly kind = signal<QueryKind>('movimientos');
  readonly tab = signal<PurchaseTab>('reposicion');
  readonly selected = signal<Row | null>(null);
  readonly results = new RemoteState<Page>(this.destroy);
  readonly metrics = new RemoteState<Row[]>(this.destroy);
  readonly stock = new RemoteState<Row>(this.destroy);
  readonly receptions = new RemoteState<Page>(this.destroy);
  readonly warehouses = new RemoteState<Warehouse[]>(this.destroy);
  readonly products = new RemoteState<Product[]>(this.destroy);
  readonly includeInactiveWarehouses = signal(false);
  readonly activeWarehouses = computed(() =>
    (this.warehouses.data() ?? []).filter((w) => w.activo),
  );
  readonly activeProducts = computed(() => (this.products.data() ?? []).filter((p) => p.activo));
  readonly queryWarehouses = computed(() =>
    this.includeInactiveWarehouses() ? (this.warehouses.data() ?? []) : this.activeWarehouses(),
  );
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
  readonly title = computed(() =>
    this.kind() === 'compras'
      ? {
          reposicion: 'Reposición',
          solicitudes: 'Solicitudes de compra',
          ordenes: 'Órdenes y recepciones',
        }[this.tab()]
      : QUERY_TITLES[this.kind()],
  );
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
      this.tab.set((data['initialTab'] as PurchaseTab) ?? 'reposicion');
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
    if (this.kind() === 'existencias' || this.role === 'COMPRAS')
      this.products.load(this.api.products());
  }
  setIncludeInactiveWarehouses(include: boolean): void {
    this.includeInactiveWarehouses.set(include);
    const selected = this.form.controls.almacenId.value;
    if (!include && this.warehouses.data()?.some((w) => String(w.id) === selected && !w.activo)) {
      this.form.controls.almacenId.setValue('');
    }
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
    this.actionError.set('');
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
  readonly actionBusy = signal(false);
  readonly actionError = signal('');
  readonly actionNotice = signal('');
  private readonly fb = inject(FormBuilder);
  readonly requestForm = this.fb.nonNullable.group({
    productoId: ['', Validators.required],
    almacenId: ['', Validators.required],
    cantidad: [1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
    montoEstimado: [
      0.01,
      [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)],
    ],
    motivo: ['', [Validators.required, Validators.maxLength(255), Validators.pattern(/\S/)]],
    urgente: false,
    motivoUrgencia: ['', Validators.maxLength(255)],
  });
  readonly decisionForm = this.fb.nonNullable.group({
    justificacion: ['', Validators.maxLength(500)],
    motivo: ['', Validators.maxLength(500)],
    originalId: '',
  });
  private requestKey = crypto.randomUUID();
  private requestSnapshot: string | null = null;
  readonly requestOpen = signal(false);
  prepare(row?: Row): void {
    if (this.role !== 'COMPRAS' || this.actionBusy()) return;
    if (
      this.requestOpen() &&
      !window.confirm('¿Descartar el borrador y preparar una nueva solicitud?')
    )
      return;
    this.requestForm.reset({
      productoId: row ? String(row['productoId']) : '',
      almacenId: row ? String(row['almacenId']) : '',
      cantidad: row ? Number(row['cantidadSugerida']) : 1,
      montoEstimado: 0.01,
      motivo: '',
      urgente: !!row?.['urgenciaSugerida'],
      motivoUrgencia: '',
    });
    this.requestKey = crypto.randomUUID();
    this.requestSnapshot = null;
    this.actionError.set('');
    this.requestOpen.set(true);
    this.detailDialog().nativeElement.close();
    this.changeTab('solicitudes');
  }
  sendRequest(): void {
    if (this.role !== 'COMPRAS' || this.actionBusy()) return;
    this.requestForm.markAllAsTouched();
    const v = this.requestForm.getRawValue();
    if (this.requestForm.invalid || (v.urgente && !v.motivoUrgencia.trim())) {
      this.actionError.set(
        'Revisa producto, almacén, cantidad, monto, motivo y motivo de urgencia.',
      );
      return;
    }
    const payload = {
      ...v,
      productoId: Number(v.productoId),
      almacenId: Number(v.almacenId),
      claveIdempotencia: this.requestKey,
    };
    const snapshot = JSON.stringify(payload);
    if (this.requestSnapshot && this.requestSnapshot !== snapshot) {
      this.actionError.set(
        'La solicitud anterior pudo haber sido recibida. Reintenta con los mismos datos o consulta el listado antes de preparar una nueva necesidad.',
      );
      return;
    }
    this.requestSnapshot = snapshot;
    this.perform(this.api.post('solicitudes-compra', payload), (row) => {
      this.requestOpen.set(false);
      this.requestForm.reset();
      this.requestKey = crypto.randomUUID();
      this.requestSnapshot = null;
      this.inspect(row);
      this.query();
    });
  }
  openSimilar(id: unknown): void {
    if (!Number.isSafeInteger(Number(id)) || Number(id) <= 0) return;
    this.perform(this.api.get<Row>(`solicitudes-compra/${Number(id)}`), (row) =>
      this.selected.set(row),
    );
  }
  decide(
    action: 'aprobar' | 'rechazar' | 'confirmar-duplicidad' | 'emitir-orden' | 'cancelar',
  ): void {
    const row = this.selected();
    if (!row || this.actionBusy()) return;
    const emit = action === 'emitir-orden';
    if ((emit && this.role !== 'COMPRAS') || (!emit && this.role !== 'SUPERVISOR')) return;
    const v = this.decisionForm.getRawValue();
    if (
      this.decisionForm.invalid ||
      (['rechazar', 'confirmar-duplicidad', 'cancelar'].includes(action) && !v.motivo.trim()) ||
      (action === 'confirmar-duplicidad' &&
        (!Number.isSafeInteger(Number(v.originalId)) || Number(v.originalId) <= 0))
    ) {
      this.actionError.set(
        'Completa un motivo válido y, para duplicidad, el ID de la solicitud original.',
      );
      return;
    }
    if (!window.confirm('¿Confirmas esta operación? Se registrará con tu cuenta.')) return;
    const payload: Row | null =
      action === 'aprobar'
        ? { justificacion: v.justificacion || null }
        : emit
          ? null
          : action === 'confirmar-duplicidad'
            ? { originalId: Number(v.originalId), motivo: v.motivo }
            : { motivo: v.motivo };
    const path = `${action === 'cancelar' ? 'ordenes' : 'solicitudes'}-compra/${Number(row['id'])}/${action}`;
    this.perform(this.api.post(path, payload), (updated) => {
      this.selected.set(updated);
      this.decisionForm.reset();
      if (emit) {
        this.closeDetail();
        this.changeTab('ordenes');
      } else this.query(this.page());
    });
  }
  private perform(request: import('rxjs').Observable<Row>, success: (row: Row) => void): void {
    this.actionBusy.set(true);
    this.actionError.set('');
    this.actionNotice.set('');
    request
      .pipe(
        timeout(30000),
        takeUntilDestroyed(this.destroy),
        finalize(() => this.actionBusy.set(false)),
      )
      .subscribe({
        next: (row) => {
          this.actionNotice.set('Operación completada.');
          success(row);
        },
        error: (error) => this.actionError.set(errorMessage(error)),
      });
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
