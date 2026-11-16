import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { WarehouseOperations } from './warehouse-operations';
import { AdminApi } from '../admin/shared/admin-api';
import { AuthService } from '../../core/auth/auth.service';

describe('Warehouse selectors in operations and historical queries', () => {
  it('includes inactive warehouses only in history and clears a hidden historical selection', () => {
    const warehouses = [
      { id: 1, codigo: 'A01', nombre: 'Activo', activo: true },
      { id: 2, codigo: 'ANTIGUO', nombre: 'Cerrado', activo: false },
      { id: 4, codigo: 'A04', nombre: 'Nuevo', activo: true },
    ];
    const api = {
      get: vi.fn((path: string) =>
        of(
          path === 'almacenes'
            ? warehouses
            : { content: [], number: 0, totalPages: 0, totalElements: 0, size: 20 },
        ),
      ),
      products: vi.fn(() => of([])),
    };
    TestBed.configureTestingModule({
      imports: [WarehouseOperations],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user: () => ({ id: 1, rol: 'ALMACEN' }) } },
        { provide: AdminApi, useValue: api },
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ operation: 'movimientos' }),
            snapshot: { queryParamMap: convertToParamMap({}) },
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(WarehouseOperations);
    fixture.detectChanges();
    const view = fixture.nativeElement as HTMLElement;
    const operation = view.querySelector(
      'form.warehouse-form select[formcontrolname="almacenId"]',
    ) as HTMLSelectElement;
    const history = view.querySelector(
      'form.ap-toolbar select[formcontrolname="almacenId"]',
    ) as HTMLSelectElement;
    const toggle = view.querySelector('input[type="checkbox"]') as HTMLInputElement;
    const values = (select: HTMLSelectElement) => [...select.options].map((option) => option.value);
    expect(values(operation)).toEqual(['', '1', '4']);
    expect(values(history)).toEqual(['', '1', '4']);
    toggle.click();
    fixture.detectChanges();
    expect(values(operation)).toEqual(['', '1', '4']);
    expect(values(history)).toEqual(['', '1', '2', '4']);
    expect(history.options[2].textContent).toContain('(inactivo)');
    history.value = '2';
    history.dispatchEvent(new Event('change'));
    fixture.componentInstance.query();
    expect(api.get).toHaveBeenLastCalledWith(
      'movimientos',
      expect.objectContaining({ almacenId: '2' }),
    );
    toggle.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.filters.controls.almacenId.value).toBe('');
    expect(fixture.componentInstance.results.data()).toBeNull();
    expect(values(history)).toEqual(['', '1', '4']);
  });
});
