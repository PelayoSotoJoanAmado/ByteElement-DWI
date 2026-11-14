import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminQueries } from './admin-queries';
import { AdminApi } from '../shared/admin-api';
import { AuthService } from '../../../core/auth/auth.service';
import { QueryKind } from './query.config';
function setup(query: QueryKind) {
  const api = {
    get: vi.fn((path: string) =>
      of(
        path === 'almacenes'
          ? []
          : path === 'existencias'
            ? { productoId: 1, almacenId: 2, cantidad: 0 }
            : path === 'indicadores'
              ? { codigo: 'I05', nombre: 'Quiebre', valor: null, sinDatos: true, unidad: '%' }
              : { content: [], number: 0, totalPages: 0, totalElements: 0, size: 20 },
      ),
    ),
    products: vi.fn(() => of([])),
  };
  TestBed.configureTestingModule({
    imports: [AdminQueries],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { user: () => ({ rol: 'ADMINISTRADOR' }) } },
      { provide: AdminApi, useValue: api },
      { provide: ActivatedRoute, useValue: { data: of({ query }) } },
    ],
  });
  const fixture = TestBed.createComponent(AdminQueries);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance, api };
}
describe('Read-only administrator queries', () => {
  it('requires both product and warehouse to query existence and keeps zero stock as zero', () => {
    const { component, api } = setup('existencias');
    component.query();
    expect(component.error()).toContain('Selecciona');
    expect(api.get.mock.calls.some((call) => call[0] === 'existencias')).toBe(false);
    component.form.patchValue({ productoId: '1', almacenId: '2' });
    component.query();
    expect(api.get).toHaveBeenCalledWith('existencias', { productoId: '1', almacenId: '2' });
    expect(component.stock.data()?.['cantidad']).toBe(0);
  });
  it('sends exact SKU, warehouse and correctly zoned dates for movements', () => {
    const { component, api } = setup('movimientos');
    component.form.patchValue({
      sku: ' abc ',
      almacenId: '2',
      desde: '2026-11-01',
      hasta: '2026-11-01',
    });
    component.query();
    expect(api.get).toHaveBeenLastCalledWith(
      'movimientos',
      expect.objectContaining({
        sku: 'ABC',
        almacenId: '2',
        desde: '2026-11-01T05:00:00.000Z',
        hasta: '2026-11-02T05:00:00.000Z',
        pagina: 0,
      }),
    );
  });
  it('does not send a report with invalid dates', () => {
    const { component, api } = setup('reportes');
    component.form.patchValue({ desde: '2026-12-01', hasta: '2026-11-01' });
    component.query();
    expect(api.get.mock.calls.some((call) => call[0].startsWith('reportes/'))).toBe(false);
    expect(component.error()).toContain('periodo válido');
  });
  it('preserves missing indicators rather than converting them to zero', () => {
    const { component, fixture } = setup('indicadores');
    component.query();
    fixture.detectChanges();
    expect(component.metrics.data()?.length).toBe(6);
    expect(fixture.nativeElement.textContent).toContain('Sin datos');
    expect(
      [...fixture.nativeElement.querySelectorAll('.ap-metric strong')].map((item: Element) =>
        item.textContent?.trim(),
      ),
    ).toEqual(Array(6).fill('—'));
  });
  it('uses only GET collection endpoints for purchase tabs', () => {
    const { component, api } = setup('compras');
    component.changeTab('solicitudes');
    component.changeTab('ordenes');
    expect(api.get.mock.calls.some((call) => call[0] === 'reposicion')).toBe(true);
    expect(api.get.mock.calls.some((call) => call[0] === 'solicitudes-compra')).toBe(true);
    expect(api.get.mock.calls.some((call) => call[0] === 'ordenes-compra')).toBe(true);
  });
  it('retains successful indicators when one calculation fails', () => {
    const { component, api } = setup('indicadores');
    api.get.mockImplementation(
      (path: string, params?: unknown) =>
        ((params as { codigo: string })?.codigo === 'I10'
          ? throwError(
              () => new HttpErrorResponse({ status: 400, error: { detail: 'Falta política' } }),
            )
          : of({
              codigo: (params as { codigo: string })?.codigo,
              valor: 10,
              sinDatos: false,
            })) as never,
    );
    component.query();
    expect(component.metrics.data()?.filter((r) => r['error']).length).toBe(1);
    expect(component.metrics.data()?.filter((r) => r['valor'] === 10).length).toBe(5);
  });
});
