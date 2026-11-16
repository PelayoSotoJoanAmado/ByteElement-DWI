import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminDashboard } from './admin-dashboard';
import { AdminApi } from '../shared/admin-api';
it('counts actual active records and alerts without turning a failed query into zero', () => {
  TestBed.configureTestingModule({
    imports: [AdminDashboard],
    providers: [
      provideRouter([]),
      {
        provide: AdminApi,
        useValue: {
          list: (kind: string) =>
            kind === 'productos'
              ? throwError(() => new HttpErrorResponse({ status: 503 }))
              : of(
                  kind === 'usuarios'
                    ? [{ activo: true, requiereCambioPassword: false, version: 0 }, { activo: true, requiereCambioPassword: false, version: 0 }, { activo: false, requiereCambioPassword: false, version: 0 }]
                    : [{ activo: true, requiereCambioPassword: false, version: 0 }],
                ),
          pageRows: () =>
            of([{ estado: 'NORMAL' }, { estado: 'QUIEBRE' }, { estado: 'SOBRE_MAXIMO' }]),
        },
      },
    ],
  });
  const component = TestBed.createComponent(AdminDashboard).componentInstance;
  expect(component.count(0)).toBe('2');
  expect(component.count(1)).toBe('—');
  expect(component.count(2)).toBe('1');
  expect(component.count(3)).toBe('2');
});
