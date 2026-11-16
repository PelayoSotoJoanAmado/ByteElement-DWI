import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, timeout } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AdminApi, Page, Product, Row, Warehouse } from '../admin/shared/admin-api';
import { DataTable } from '../admin/shared/data-table';
import { RecordDetail } from '../admin/shared/record-detail';
import { RemoteState, errorMessage } from '../admin/shared/remote-state';
import { daysBefore, periodParams, todayLima } from '../admin/shared/presentation';
@Component({
  selector: 'app-supervisor-control',
  imports: [ReactiveFormsModule, DataTable, RecordDetail],
  template: `<header class="ap-page-header">
      <div>
        <h1 class="ap-title">{{ title() }}</h1>
        <p class="ap-subtitle">Control de inventario y trazabilidad.</p>
      </div>
    </header>
    @if (error()) {
      <p class="ap-notice ap-error" role="alert">{{ error() }}</p>
    }
    @if (notice()) {
      <p class="ap-notice" role="status">{{ notice() }}</p>
    }
    @if (products.error() || warehouses.error()) {
      <p class="ap-notice ap-error" role="alert">{{ products.error() || warehouses.error() }}</p>
      <button class="ap-button" (click)="loadOptions()">Reintentar opciones</button>
    }
    @if (kind() === 'politicas') {
      <section class="ap-panel">
        <form [formGroup]="policy" (ngSubmit)="savePolicy()" class="ap-form-grid">
          <label
            >Producto<select class="ap-select" formControlName="productoId">
              <option value="">Selecciona producto</option>
              @for (p of products.data() ?? []; track p.id) {
                <option [value]="p.id">{{ p.sku }} · {{ p.modelo }}</option>
              }
            </select></label
          >
          <label
            >Almacén<select class="ap-select" formControlName="almacenId">
              <option value="">Selecciona almacén</option>
              @for (w of warehouses.data() ?? []; track w.id) {
                <option [value]="w.id">{{ w.codigo }}</option>
              }
            </select></label
          >
          <label
            >Mínimo<input class="ap-input" type="number" min="0" step="1" formControlName="minimo"
          /></label>
          <label
            >Máximo<input class="ap-input" type="number" min="1" step="1" formControlName="maximo"
          /></label>
          <div class="ap-actions">
            <button class="ap-button" type="button" [disabled]="busy()" (click)="loadPolicy()">
              Consultar política</button
            ><button class="ap-button ap-primary" [disabled]="busy()">Guardar política</button>
          </div>
        </form>
        @if (selected()) {
          <app-record-detail [record]="selected()!" />
        }
      </section>
    }
    @if (kind() === 'cortes') {
      <section class="ap-panel">
        <p>Consulta un lote conocido por su identificador. La API no lista todos los lotes.</p>
        <form [formGroup]="cut" (ngSubmit)="loadCut()" class="ap-toolbar">
          <label>Lote<input class="ap-input" formControlName="lote" maxlength="80" /></label
          ><button class="ap-button" [disabled]="busy()">Consultar lote</button>
          @if (supervisor) {
            <button
              class="ap-button ap-primary"
              type="button"
              [disabled]="busy()"
              (click)="generateCut()"
            >
              Generar corte de stock
            </button>
          }
        </form>
        <app-data-table [rows]="cutRows()" [columns]="cutColumns" caption="Corte de stock" />
      </section>
    }
    @if (kind() === 'conteos') {
      <section class="ap-panel">
        <form [formGroup]="filters" (ngSubmit)="loadCounts()" class="ap-toolbar">
          <label>SKU<input class="ap-input" formControlName="sku" /></label
          ><label
            >Almacén<select class="ap-select" formControlName="almacenId">
              <option value="">Todos</option>
              @for (w of warehouses.data() ?? []; track w.id) {
                <option [value]="w.id">{{ w.codigo }}</option>
              }
            </select></label
          >
          <label
            >Estado<select class="ap-select" formControlName="confirmado">
              <option value="">Todos</option>
              <option value="false">Pendientes</option>
              <option value="true">Confirmados</option>
            </select></label
          >
          <label>Desde<input class="ap-input" type="date" formControlName="desde" /></label
          ><label
            >Hasta (inclusive)<input class="ap-input" type="date" formControlName="hasta" /></label
          ><button class="ap-button" [disabled]="busy()">Consultar</button>
        </form>
        @if (counts.data(); as page) {
          <app-data-table
            [rows]="page.content"
            [columns]="countColumns"
            [detail]="true"
            (inspect)="selected.set($event)"
            caption="Conteos físicos"
          />
          <div class="ap-pager">
            <button
              class="ap-button"
              [disabled]="busy() || page.number === 0"
              (click)="loadCounts(page.number - 1)"
            >
              Anterior</button
            ><span>Página {{ page.number + 1 }} · {{ page.totalElements }} resultados</span
            ><button
              class="ap-button"
              [disabled]="busy() || page.number + 1 >= page.totalPages"
              (click)="loadCounts(page.number + 1)"
            >
              Siguiente
            </button>
          </div>
        }
        @if (counts.error()) {
          <p class="ap-notice ap-error" role="alert">{{ counts.error() }}</p>
        }
        @if (selected(); as row) {
          <h2>Comparación al corte</h2>
          <app-record-detail [record]="row" />
          @if (!row['confirmado']) {
            <button class="ap-button ap-primary" [disabled]="busy()" (click)="confirmCount(row)">
              Confirmar ajuste
            </button>
          }
          <p class="ap-help">
            Si hubo movimientos posteriores, el servidor rechazará el ajuste con 409. Consulta un
            conteo nuevo; no se fuerza el anterior.
          </p>
        }
      </section>
    }`,
})
export class SupervisorControl {
  private readonly api = inject(AdminApi);
  private readonly destroy = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  readonly supervisor = inject(AuthService).user()?.rol === 'SUPERVISOR';
  readonly kind = signal('politicas');
  readonly title = signal('Políticas min/max');
  readonly selected = signal<Row | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly products = new RemoteState<Product[]>(this.destroy);
  readonly warehouses = new RemoteState<Warehouse[]>(this.destroy);
  readonly counts = new RemoteState<Page>(this.destroy);
  readonly cutRows = signal<Row[]>([]);
  readonly countColumns = [
    { key: 'id', label: 'Conteo' },
    { key: 'productoId', label: 'Producto' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'cantidadFisica', label: 'Físico' },
    { key: 'saldoCorte', label: 'Saldo al corte' },
    { key: 'confirmado', label: 'Confirmado' },
    { key: 'fechaCorte', label: 'Fecha', format: 'date' },
  ];
  readonly cutColumns = [
    { key: 'lote', label: 'Lote' },
    { key: 'fecha', label: 'Fecha', format: 'date' },
    { key: 'sku', label: 'SKU' },
    { key: 'almacenId', label: 'Almacén' },
    { key: 'cantidad', label: 'Cantidad' },
    { key: 'minimo', label: 'Mínimo' },
    { key: 'maximo', label: 'Máximo' },
  ];
  readonly policy = this.fb.nonNullable.group({
    productoId: ['', Validators.required],
    almacenId: ['', Validators.required],
    minimo: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    maximo: [1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
  });
  readonly cut = this.fb.nonNullable.group({ lote: ['', Validators.required] });
  readonly filters = this.fb.nonNullable.group({
    sku: '',
    almacenId: '',
    confirmado: 'false',
    desde: daysBefore(todayLima(), 29),
    hasta: todayLima(),
  });
  constructor() {
    this.policy.controls.productoId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.selected.set(null));
    this.policy.controls.almacenId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.selected.set(null));
    inject(ActivatedRoute)
      .data.pipe(takeUntilDestroyed())
      .subscribe((d) => {
        this.kind.set(d['control']);
        this.title.set(d['label']);
        this.selected.set(null);
        this.error.set('');
        if (this.kind() !== 'cortes') this.loadOptions();
        if (this.kind() === 'conteos') this.loadCounts();
      });
  }
  loadOptions(): void {
    this.warehouses.load(this.api.get<Warehouse[]>('almacenes'));
    if (this.kind() === 'politicas') this.products.load(this.api.products());
  }
  private policyPath(): string | null {
    const v = this.policy.getRawValue();
    if (!v.productoId || !v.almacenId) {
      this.error.set('Selecciona producto y almacén.');
      return null;
    }
    return `productos/${Number(v.productoId)}/almacenes/${Number(v.almacenId)}/politica-stock`;
  }
  loadPolicy(): void {
    const path = this.policyPath();
    if (!path || this.busy()) return;
    this.run(this.api.get<Row>(path), (row) => {
      this.selected.set(row);
      this.policy.patchValue({ minimo: Number(row['minimo']), maximo: Number(row['maximo']) });
    });
  }
  savePolicy(): void {
    if (!this.supervisor || this.busy()) return;
    this.policy.markAllAsTouched();
    const v = this.policy.getRawValue();
    if (this.policy.invalid || v.maximo <= v.minimo) {
      this.error.set('Usa cantidades enteras: mínimo ≥ 0 y máximo mayor al mínimo.');
      return;
    }
    const path = this.policyPath();
    if (path)
      this.run(this.api.put<Row>(path, { minimo: v.minimo, maximo: v.maximo }), (row) => {
        this.selected.set(row);
        this.notice.set('Política guardada.');
      });
  }
  loadCut(): void {
    if (this.cut.invalid || this.busy()) return;
    this.run(
      this.api.get<Row[]>(`cortes-stock/${encodeURIComponent(this.cut.getRawValue().lote.trim())}`),
      (rows) => this.cutRows.set(rows),
    );
  }
  generateCut(): void {
    if (
      !this.supervisor ||
      this.busy() ||
      !window.confirm('¿Generar una fotografía del stock actual?')
    )
      return;
    this.run(this.api.postRows('cortes-stock', null), (rows) => {
      this.cutRows.set(rows);
      this.cut.controls.lote.setValue(String(rows[0]?.['lote'] ?? ''));
      this.notice.set('Corte generado. Conserva el identificador del lote.');
    });
  }
  loadCounts(page = 0): void {
    if (this.busy()) return;
    const v = this.filters.getRawValue();
    this.selected.set(null);
    try {
      this.counts.load(
        this.api.get<Page>('conteos', {
          ...v,
          ...periodParams(v.desde, v.hasta),
          pagina: page,
          tamanio: 20,
        }),
      );
    } catch (e) {
      this.error.set((e as Error).message);
    }
  }
  confirmCount(row: Row): void {
    if (
      !this.supervisor ||
      row['confirmado'] ||
      this.busy() ||
      !window.confirm('¿Confirmar este ajuste con los valores mostrados?')
    )
      return;
    this.run(this.api.post(`conteos/${Number(row['id'])}/confirmar`, null), (updated) => {
      this.loadCounts(this.counts.data()?.number ?? 0);
      this.selected.set(updated);
      this.notice.set('Ajuste confirmado.');
    });
  }
  private run<T>(request: import('rxjs').Observable<T>, success: (value: T) => void): void {
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    request
      .pipe(
        timeout(30000),
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (value) => {
          this.busy.set(false);
          success(value);
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
  }
}
