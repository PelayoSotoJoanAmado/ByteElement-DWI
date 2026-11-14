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
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, finalize, timeout, switchMap, map } from 'rxjs';
import { AdminApi, Page, Params, Product, Row, Warehouse } from '../admin/shared/admin-api';
import { RemoteState, errorMessage } from '../admin/shared/remote-state';
import { Column, DataTable } from '../admin/shared/data-table';
import { RecordDetail } from '../admin/shared/record-detail';
import { display, daysBefore, todayLima, periodParams } from '../admin/shared/presentation';
import { QUERY_COLUMNS } from '../admin/queries/query.config';
import { AuthService } from '../../core/auth/auth.service';
type Operation = 'movimientos' | 'transferencias' | 'conteos' | 'recepciones';
interface PendingReceipt {
  orderId: number;
  payload: Row;
}
const COUNT_COLUMNS: Column[] = [
  { key: 'id', label: 'Conteo' },
  { key: 'fechaCorte', label: 'Fecha (Lima)', format: 'date' },
  { key: 'productoId', label: 'Producto' },
  { key: 'almacenId', label: 'Almacén' },
  { key: 'cantidadFisica', label: 'Cantidad física' },
  { key: 'saldoCorte', label: 'Saldo al corte' },
  { key: 'confirmado', label: 'Confirmado' },
];
@Component({
  selector: 'app-warehouse-operations',
  imports: [ReactiveFormsModule, RouterLink, DataTable, RecordDetail],
  templateUrl: './warehouse-operations.html',
  styleUrl: './warehouse.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WarehouseOperations {
  private readonly api = inject(AdminApi);
  private readonly destroy = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly receiptStorageKey = `stockguard.receipt.${this.auth.user()?.id}`;
  private subscription?: Subscription;
  private pendingReceipt: PendingReceipt | null = null;
  readonly kind = signal<Operation>('movimientos');
  readonly title = computed(
    () =>
      ({
        movimientos: 'Entradas y salidas',
        transferencias: 'Transferencias',
        conteos: 'Conteos físicos',
        recepciones: 'Recepciones de compra',
      })[this.kind()],
  );
  readonly products = new RemoteState<Product[]>(this.destroy);
  readonly warehouses = new RemoteState<Warehouse[]>(this.destroy);
  readonly results = new RemoteState<Page>(this.destroy);
  readonly order = new RemoteState<Row>(this.destroy);
  readonly receipts = new RemoteState<Page>(this.destroy);
  readonly detailDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailDialog');
  readonly selected = signal<Row | null>(null);
  readonly busy = signal(false);
  readonly locked = signal(false);
  readonly uncertain = signal(false);
  readonly failure = signal('');
  readonly notice = signal('');
  readonly result = signal<Row | null>(null);
  readonly page = signal(0);
  readonly receiptPage = signal(0);
  readonly queryError = signal('');
  readonly show = display;
  readonly activeProducts = computed(() => (this.products.data() ?? []).filter((p) => p.activo));
  readonly activeWarehouses = computed(() =>
    (this.warehouses.data() ?? []).filter((w) => w.activo),
  );
  readonly columns = computed(() =>
    this.kind() === 'conteos'
      ? COUNT_COLUMNS
      : QUERY_COLUMNS[this.kind() === 'recepciones' ? 'ordenes' : 'movimientos'],
  );
  readonly receptionColumns = QUERY_COLUMNS['recepciones'];
  readonly form = this.fb.nonNullable.group({
    productoId: '',
    almacenId: '',
    destinoAlmacenId: '',
    tipo: 'ENTRADA',
    cantidad: '',
    motivo: '',
    ordenId: '',
  });
  readonly filters = this.fb.nonNullable.group({
    sku: '',
    almacenId: '',
    desde: daysBefore(todayLima(), 29),
    hasta: todayLima(),
    estado: '',
    confirmado: '',
  });
  readonly canReceive = computed(() => {
    const o = this.order.data();
    return (
      !!o &&
      o['estado'] === 'EMITIDA' &&
      Number(o['cantidadPendiente']) > 0 &&
      Number(o['id']) === Number(this.form.controls.ordenId.value)
    );
  });
  constructor() {
    this.destroy.onDestroy(() => this.subscription?.unsubscribe());
    this.filters.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.results.clear();
      this.page.set(0);
      this.queryError.set('');
    });
    this.form.controls.ordenId.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.order.clear();
      this.receipts.clear();
    });
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      this.kind.set(data['operation'] as Operation);
      this.configureForm();
      this.loadOptions();
      if (this.kind() === 'recepciones')
        this.filters.patchValue({ estado: 'EMITIDA', desde: '1970-01-01' }, { emitEvent: false });
      if (this.kind() !== 'transferencias') this.query();
      if (this.kind() === 'recepciones') {
        this.restoreReceipt();
        if (!this.pendingReceipt)
          this.form.controls.ordenId.setValue(this.route.snapshot.queryParamMap.get('orden') ?? '');
        if (this.form.controls.ordenId.value) this.loadOrder();
      }
    });
  }
  private configureForm(): void {
    for (const control of Object.values(this.form.controls)) {
      control.clearValidators();
      control.updateValueAndValidity({ emitEvent: false });
    }
    const required =
      this.kind() === 'recepciones'
        ? ['ordenId', 'cantidad', 'motivo']
        : this.kind() === 'transferencias'
          ? ['productoId', 'almacenId', 'destinoAlmacenId', 'cantidad', 'motivo']
          : ['productoId', 'almacenId', 'cantidad', 'motivo'];
    for (const key of required) this.form.get(key)!.setValidators([Validators.required]);
    this.form.controls.motivo.addValidators([Validators.maxLength(500), Validators.pattern(/\S/)]);
    for (const control of Object.values(this.form.controls))
      control.updateValueAndValidity({ emitEvent: false });
    this.form.updateValueAndValidity({ emitEvent: false });
  }
  loadOptions(): void {
    if (this.kind() !== 'recepciones') this.products.load(this.api.products());
    this.warehouses.load(this.api.get<Warehouse[]>('almacenes'));
  }
  query(page = 0): void {
    if (this.kind() === 'transferencias') return;
    const f = this.filters.getRawValue();
    const params: Params = { sku: f.sku.trim(), almacenId: f.almacenId, pagina: page, tamanio: 20 };
    this.queryError.set('');
    try {
      Object.assign(params, periodParams(f.desde, f.hasta));
    } catch (e) {
      this.queryError.set((e as Error).message);
      return;
    }
    if (this.kind() === 'recepciones') params['estado'] = f.estado;
    if (this.kind() === 'conteos' && f.confirmado !== '')
      params['confirmado'] = f.confirmado === 'true';
    this.page.set(page);
    this.results.load(
      this.api.get<Page>(this.kind() === 'recepciones' ? 'ordenes-compra' : this.kind(), params),
    );
  }
  inspect(row: Row): void {
    this.selected.set(row);
    this.detailDialog().nativeElement.showModal();
  }
  chooseOrder(row: Row): void {
    if (this.locked() || this.busy()) return;
    this.form.controls.ordenId.setValue(String(row['id']));
    this.loadOrder();
    document
      .getElementById('warehouse-operation')
      ?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  }
  loadOrder(): void {
    const id = Number(this.form.controls.ordenId.value);
    if (!Number.isSafeInteger(id) || id <= 0) {
      this.failure.set('Indica un ID de orden válido.');
      return;
    }
    if (!this.uncertain()) this.failure.set('');
    this.order.load(
      this.api.get<Row>(`ordenes-compra/${id}`).pipe(
        switchMap((order) =>
          this.api.get<Row>(`solicitudes-compra/${Number(order['solicitudId'])}`).pipe(
            map((request) => ({
              ...order,
              sku: request['sku'],
              productoId: request['productoId'],
              almacenId: request['almacenId'],
            })),
          ),
        ),
      ),
    );
    this.loadReceipts(0);
  }
  warehouseLabel(id: unknown): string {
    const warehouse = this.warehouses.data()?.find((w) => w.id === Number(id));
    return warehouse ? `${warehouse.codigo} · ${warehouse.nombre}` : String(id);
  }
  loadReceipts(page: number): void {
    const id = Number(this.form.controls.ordenId.value);
    if (!Number.isSafeInteger(id) || id <= 0) return;
    this.receiptPage.set(page);
    this.receipts.load(
      this.api.get<Page>(`ordenes-compra/${id}/recepciones`, { pagina: page, tamanio: 20 }),
    );
  }
  private restoreReceipt(): void {
    try {
      const raw = sessionStorage.getItem(this.receiptStorageKey);
      if (!raw) return;
      const pending = JSON.parse(raw) as PendingReceipt;
      if (
        !Number.isSafeInteger(pending.orderId) ||
        pending.orderId <= 0 ||
        !pending.payload ||
        typeof pending.payload['claveIdempotencia'] !== 'string' ||
        !Number.isSafeInteger(pending.payload['cantidad']) ||
        Number(pending.payload['cantidad']) <= 0 ||
        typeof pending.payload['motivo'] !== 'string'
      )
        return;
      this.pendingReceipt = pending;
      this.form.patchValue({
        ordenId: String(pending.orderId),
        cantidad: String(pending.payload['cantidad']),
        motivo: String(pending.payload['motivo']),
      });
      this.locked.set(true);
      this.uncertain.set(true);
      this.form.disable({ emitEvent: false });
      this.failure.set(
        'Hay una recepción sin respuesta confirmada. Revisa su historial o reintenta la misma recepción.',
      );
    } catch {
      /* Storage unavailable: the current form still retains its retry key. */
    }
  }
  submit(): void {
    if (this.busy() || this.locked()) return;
    this.form.markAllAsTouched();
    this.failure.set('');
    this.result.set(null);
    this.notice.set('');
    const f = this.form.getRawValue();
    const quantity = Number(f.cantidad);
    if (
      this.form.invalid ||
      String(f.cantidad ?? '').trim() === '' ||
      !Number.isSafeInteger(quantity) ||
      quantity < (this.kind() === 'conteos' ? 0 : 1)
    ) {
      this.failure.set(
        'Completa los campos obligatorios. La cantidad debe ser un número entero válido.',
      );
      return;
    }
    let payload: Row = { motivo: f.motivo.trim() };
    let path: string = this.kind();
    if (this.kind() === 'recepciones') {
      if (!this.canReceive() || quantity > Number(this.order.data()?.['cantidadPendiente'])) {
        this.failure.set(
          'Carga una orden emitida e indica una cantidad que no supere lo pendiente.',
        );
        return;
      }
      payload = { ...payload, cantidad: quantity, claveIdempotencia: crypto.randomUUID() };
      this.pendingReceipt = { orderId: Number(f.ordenId), payload };
      try {
        sessionStorage.setItem(this.receiptStorageKey, JSON.stringify(this.pendingReceipt));
      } catch {
        /* In-memory retry remains available. */
      }
      path = `ordenes-compra/${Number(f.ordenId)}/recepciones`;
    } else {
      const product = Number(f.productoId);
      const warehouse = Number(f.almacenId);
      if (
        !this.activeProducts().some((p) => p.id === product) ||
        !this.activeWarehouses().some((w) => w.id === warehouse)
      ) {
        this.failure.set('Selecciona un producto y un almacén activos.');
        return;
      }
      payload['productoId'] = product;
      if (this.kind() === 'transferencias') {
        const destination = Number(f.destinoAlmacenId);
        if (
          destination === warehouse ||
          !this.activeWarehouses().some((w) => w.id === destination)
        ) {
          this.failure.set('El destino debe ser un almacén activo diferente al origen.');
          return;
        }
        Object.assign(payload, {
          origenAlmacenId: warehouse,
          destinoAlmacenId: destination,
          cantidad: quantity,
        });
      } else {
        payload['almacenId'] = warehouse;
        if (this.kind() === 'conteos') payload['cantidadFisica'] = quantity;
        else {
          if (!['ENTRADA', 'SALIDA'].includes(f.tipo)) {
            this.failure.set('Selecciona entrada o salida.');
            return;
          }
          Object.assign(payload, { cantidad: quantity, tipo: f.tipo });
        }
      }
    }
    this.send(path, payload);
  }
  retryReceipt(): void {
    if (!this.pendingReceipt || this.busy()) return;
    this.send(
      `ordenes-compra/${this.pendingReceipt.orderId}/recepciones`,
      this.pendingReceipt.payload,
    );
  }
  private send(path: string, payload: Row): void {
    this.busy.set(true);
    this.locked.set(true);
    this.form.disable({ emitEvent: false });
    this.failure.set('');
    this.subscription = this.api
      .post(path, payload)
      .pipe(
        timeout(30000),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (row) => {
          this.pendingReceipt = null;
          try {
            if (this.kind() === 'recepciones') sessionStorage.removeItem(this.receiptStorageKey);
          } catch {
            /* Optional retry persistence. */
          }
          this.uncertain.set(false);
          this.result.set(row);
          this.notice.set(
            this.kind() === 'conteos'
              ? 'Conteo registrado. El supervisor debe revisarlo y confirmarlo.'
              : 'Operación registrada correctamente.',
          );
          if (this.kind() === 'recepciones') this.loadOrder();
          if (this.kind() !== 'transferencias') this.query(this.page());
        },
        error: (error) => {
          // A lost response may already have committed. Only receipt retries have backend idempotency.
          const definitive =
            error instanceof HttpErrorResponse &&
            [400, 401, 403, 404, 409, 422].includes(error.status);
          this.uncertain.set(!definitive);
          this.failure.set(
            definitive
              ? errorMessage(error)
              : 'No se pudo confirmar el resultado. Revisa el historial antes de registrar otra operación.',
          );
          if (definitive) {
            this.pendingReceipt = null;
            try {
              if (this.kind() === 'recepciones') sessionStorage.removeItem(this.receiptStorageKey);
            } catch {
              /* Optional storage. */
            }
            this.locked.set(false);
            this.form.enable({ emitEvent: false });
          }
        },
      });
  }
  newOperation(): void {
    if (this.busy()) return;
    if (
      this.uncertain() &&
      !window.confirm(
        'El resultado anterior no está confirmado. Comprueba el historial antes de continuar. ¿Ya verificaste la operación y deseas iniciar otra?',
      )
    )
      return;
    this.pendingReceipt = null;
    try {
      if (this.kind() === 'recepciones') sessionStorage.removeItem(this.receiptStorageKey);
    } catch {
      /* Optional storage. */
    }
    this.locked.set(false);
    this.uncertain.set(false);
    this.failure.set('');
    this.notice.set('');
    this.result.set(null);
    this.form.enable({ emitEvent: false });
    this.form.reset({ tipo: 'ENTRADA' });
    this.order.clear();
    this.receipts.clear();
  }
  canLeave(): boolean {
    if (!this.auth.session()) return true;
    if (this.busy()) return false;
    return (
      !this.uncertain() ||
      window.confirm(
        'El resultado de una operación no está confirmado. Revisa su historial antes de volver a registrarla. ¿Deseas salir de esta pantalla?',
      )
    );
  }
}
