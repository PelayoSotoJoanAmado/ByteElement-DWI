import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminQueries } from '../admin/queries/admin-queries';
import { SupervisorControl } from './supervisor-control';
import { AdminApi, Row } from '../admin/shared/admin-api';
import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/auth/auth.models';
function setup(role: Role, control?: string) {
  const empty = { content: [], totalPages: 0, totalElements: 0, number: 0, size: 20 };
  const api = {
    get: vi.fn((path: string) => of(path === 'almacenes' ? [] : empty)),
    products: vi.fn(() => of([])),
    post: vi.fn(() => of({ id: 1, estado: 'APROBADA' } as Row)),
    put: vi.fn(() => of({ minimo: 2, maximo: 10 })),
    postRows: vi.fn(() => of([{ lote: 'known-cut', sku: 'RAM' }])),
  };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { user: () => ({ id: 1, rol: role }) } },
      { provide: AdminApi, useValue: api },
      {
        provide: ActivatedRoute,
        useValue: { data: of(control ? { control, label: control } : { query: 'compras' }) },
      },
    ],
  });
  const fixture = control
    ? TestBed.createComponent(SupervisorControl)
    : TestBed.createComponent(AdminQueries);
  fixture.detectChanges();
  return { fixture, api, c: fixture.componentInstance };
}
describe('Purchase and supervision role actions (simulated API)', () => {
  beforeEach(() => {
    HTMLDialogElement.prototype.showModal = function () {
      this.open = true;
    };
    HTMLDialogElement.prototype.close = function () {
      this.open = false;
    };
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });
  afterEach(() => vi.restoreAllMocks());
  it('prepares a suggested request without sending anything', () => {
    const { c, api } = setup('COMPRAS');
    const q = c as AdminQueries;
    q.prepare({ productoId: 2, almacenId: 3, cantidadSugerida: 12, urgenciaSugerida: true });
    expect(api.post).not.toHaveBeenCalled();
    expect(q.tab()).toBe('solicitudes');
    expect(q.requestForm.getRawValue()).toEqual(
      expect.objectContaining({ productoId: '2', almacenId: '3', cantidad: 12, urgente: true }),
    );
  });
  it('preserves an idempotency key after a timeout and prevents duplicate submits', () => {
    const { c, api } = setup('COMPRAS');
    const q = c as AdminQueries;
    q.prepare();
    q.requestForm.setValue({
      productoId: '2',
      almacenId: '3',
      cantidad: 12,
      montoEstimado: 120,
      motivo: 'Reposición',
      urgente: false,
      motivoUrgencia: '',
    });
    const pending = new Subject<Row>();
    api.post.mockReturnValue(pending as never);
    q.sendRequest();
    q.sendRequest();
    expect(api.post).toHaveBeenCalledTimes(1);
    const first = api.post.mock.calls[0] as unknown as [string, Row];
    expect(first[0]).toBe('solicitudes-compra');
    pending.error(new HttpErrorResponse({ status: 0 }));
    api.post.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })) as never);
    q.sendRequest();
    expect(api.post.mock.calls[1]).toEqual(first);
    q.requestForm.patchValue({ cantidad: 13 });
    q.sendRequest();
    expect(api.post).toHaveBeenCalledTimes(2);
    expect(q.actionError()).toContain('mismos datos');
  });
  it('emits an order only through the approved request API', () => {
    const { c, api } = setup('COMPRAS');
    const q = c as AdminQueries;
    q.selected.set({ id: 5, estado: 'APROBADA' });
    q.decide('emitir-orden');
    expect(api.post).toHaveBeenCalledWith('solicitudes-compra/5/emitir-orden', null);
    expect(q.tab()).toBe('ordenes');
  });
  it.each<Role>(['ADMINISTRADOR', 'ALMACEN', 'CONSULTA_COMERCIAL'])(
    'does not submit operational writes as %s',
    (role) => {
      const { c, api } = setup(role);
      const q = c as AdminQueries;
      q.selected.set({ id: 5, estado: 'APROBADA' });
      q.decide('emitir-orden');
      q.decide('aprobar');
      q.prepare();
      q.sendRequest();
      expect(api.post).not.toHaveBeenCalled();
    },
  );
  it('sends supervisor justification, duplicate original, rejection and cancellation reasons', () => {
    const { c, api } = setup('SUPERVISOR');
    const q = c as AdminQueries;
    for (const action of ['aprobar', 'rechazar', 'confirmar-duplicidad', 'cancelar'] as const) {
      q.selected.set({ id: 5, estado: action === 'cancelar' ? 'EMITIDA' : 'PENDIENTE' });
      q.decisionForm.setValue({
        justificacion: 'Monto autorizado',
        motivo: 'Revisión documentada',
        originalId: '2',
      });
      q.decide(action);
    }
    expect(api.post).toHaveBeenCalledWith('solicitudes-compra/5/aprobar', {
      justificacion: 'Monto autorizado',
    });
    expect(api.post).toHaveBeenCalledWith('solicitudes-compra/5/rechazar', {
      motivo: 'Revisión documentada',
    });
    expect(api.post).toHaveBeenCalledWith('solicitudes-compra/5/confirmar-duplicidad', {
      originalId: 2,
      motivo: 'Revisión documentada',
    });
    expect(api.post).toHaveBeenCalledWith('ordenes-compra/5/cancelar', {
      motivo: 'Revisión documentada',
    });
  });
  it('validates min/max and saves a policy for the selected pair', () => {
    const { c, api } = setup('SUPERVISOR', 'politicas');
    const q = c as SupervisorControl;
    q.policy.setValue({ productoId: '2', almacenId: '3', minimo: 10, maximo: 5 });
    q.savePolicy();
    expect(api.put).not.toHaveBeenCalled();
    q.policy.patchValue({ maximo: 20 });
    q.savePolicy();
    expect(api.put).toHaveBeenCalledWith('productos/2/almacenes/3/politica-stock', {
      minimo: 10,
      maximo: 20,
    });
  });
  it('keeps a stale-count 409 visible and does not retry an adjustment', () => {
    const { c, api } = setup('SUPERVISOR', 'conteos');
    const q = c as SupervisorControl;
    api.post.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 409, error: { detail: 'Conteo obsoleto' } }),
      ) as never,
    );
    q.confirmCount({ id: 5, confirmado: false });
    expect(api.post).toHaveBeenCalledTimes(1);
    expect(q.error()).toBe('Conteo obsoleto');
  });
  it('generates a stock cut and keeps its returned lot available', () => {
    const { c, api } = setup('SUPERVISOR', 'cortes');
    const q = c as SupervisorControl;
    q.generateCut();
    expect(api.postRows).toHaveBeenCalledWith('cortes-stock', null);
    expect(q.cut.controls.lote.value).toBe('known-cut');
  });
  it('allows administrator cut lookup but never generation', () => {
    const { c, api } = setup('ADMINISTRADOR', 'cortes');
    const q = c as SupervisorControl;
    q.generateCut();
    expect(api.postRows).not.toHaveBeenCalled();
    q.cut.controls.lote.setValue('known-cut');
    q.loadCut();
    expect(api.get).toHaveBeenCalledWith('cortes-stock/known-cut');
  });
});
