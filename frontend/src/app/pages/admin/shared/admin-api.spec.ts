import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminApi } from './admin-api';
import { daysBefore, periodParams } from './presentation';
describe('Administrator API contracts', () => {
  let api: AdminApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AdminApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('reads all catalog pages without truncating at 100 products', () => {
    let count = 0;
    api.products().subscribe((rows) => (count = rows.length));
    const one = http.expectOne(
      (r) => r.url === '/api/v1/productos' && r.params.get('pagina') === '0',
    );
    one.flush(Array.from({ length: 100 }, (_, id) => ({ id, sku: `SKU-${id}` })));
    http
      .expectOne((r) => r.url === '/api/v1/productos' && r.params.get('pagina') === '1')
      .flush([{ id: 100, sku: 'LAST' }]);
    expect(count).toBe(101);
  });
  it('aggregates all Spring pages for alert counts', () => {
    let count = 0;
    api.pageRows('reposicion').subscribe((rows) => (count = rows.length));
    http
      .expectOne((r) => r.url === '/api/v1/reposicion' && r.params.get('pagina') === '0')
      .flush({ content: [{ id: 1 }], number: 0, totalPages: 2, totalElements: 2 });
    http
      .expectOne((r) => r.params.get('pagina') === '1')
      .flush({ content: [{ id: 2 }], number: 1, totalPages: 2, totalElements: 2 });
    expect(count).toBe(2);
  });
  it('uses POST, PUT and PATCH for supported catalog operations', () => {
    api.save('productos', { sku: 'A' }).subscribe();
    const post = http.expectOne('/api/v1/productos');
    expect(post.request.method).toBe('POST');
    post.flush({});
    api.save('almacenes', { codigo: 'A', nombre: 'Principal' }, 2).subscribe();
    const put = http.expectOne('/api/v1/almacenes/2');
    expect(put.request.method).toBe('PUT');
    put.flush({});
    api.deactivate('productos', 3).subscribe();
    const patch = http.expectOne('/api/v1/productos/3/desactivar');
    expect(patch.request.method).toBe('PATCH');
    patch.flush({});
  });
  it('omits blank filters and preserves page zero', () => {
    api.get('movimientos', { sku: '', almacenId: null, pagina: 0, tamanio: 20 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/movimientos');
    expect(req.request.params.has('sku')).toBe(false);
    expect(req.request.params.get('pagina')).toBe('0');
    req.flush({});
  });
});
describe('Reporting periods in Lima', () => {
  it('converts inclusive local dates to an exclusive UTC end', () =>
    expect(periodParams('2026-11-01', '2026-11-30')).toEqual({
      desde: '2026-11-01T05:00:00.000Z',
      hasta: '2026-12-01T05:00:00.000Z',
    }));
  it('accepts a single full day across leap years', () => {
    expect(periodParams('2024-02-29', '2024-02-29').hasta).toBe('2024-03-01T05:00:00.000Z');
    expect(daysBefore('2026-01-01', 1)).toBe('2025-12-31');
  });
  it('rejects inverted or invalid dates before sending a request', () => {
    expect(() => periodParams('2026-03-01', '2026-02-01')).toThrow();
    expect(() => periodParams('2026-02-30', '2026-03-01')).toThrow();
    expect(() => periodParams('', '')).toThrow();
  });
});
